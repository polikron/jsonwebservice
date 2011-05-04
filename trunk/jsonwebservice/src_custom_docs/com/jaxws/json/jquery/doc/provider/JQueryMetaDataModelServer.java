package com.jaxws.json.jquery.doc.provider;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.jaxws.json.codec.JSONBindingID;
import com.jaxws.json.codec.JSONCodec;
import com.jaxws.json.codec.decode.JSONReader;
import com.jaxws.json.codec.doc.HttpMetadataProvider;
import com.jaxws.json.codec.doc.JSONHttpMetadataPublisher;
import com.jaxws.json.codec.encode.WSJSONWriter;
import com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.server.BoundEndpoint;
import com.sun.xml.ws.api.server.Module;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.WSHTTPConnection;

/**
 * @author Sundaramurthi Saminathan
 * @since JSONWebservice codec version 0.7
 * @version 1.0
 * 
 * JQuery JSON service end point document provider.
 */
public class JQueryMetaDataModelServer implements HttpMetadataProvider {
	
	private static final String[] queries = new String[]{"jsonmodel"};
	
	/**
	 * Map holder which keeps end point documents.
	 */
	private final static Map<QName,String>	endPointDocuments	= Collections.synchronizedMap(new HashMap<QName,String>());
	
	/**
	 * Request received codec instance holder
	 */
	private JSONCodec codec;
	
	
	/**
	 * "jsonmodel" query handled.
	 */
	public String[] getHandlingQueries() {
		return queries;
	}

	/**
	 * Handler flag, If query string is jsonmodel , its handled by model server.
	 */
	public boolean canHandle(String queryString) {
		return queryString != null && queryString.startsWith(queries[0]);
	}
	
	/**
	 * end point codec set holder.
	 */
	public void setJSONCodec(JSONCodec codec) {
		this.codec	= codec;
	}
	
	public void setHttpAdapter(HttpAdapter httpAdapter) {
		// TODO Auto-generated method stub

	}

	/**
	 * Meta data model content provider.
	 * @see HttpMetadataProvider.getContentType
	 */
	public String getContentType() {
		return "application/json; charset=\"utf-8\"";
	}

	public void process() {
		Map<String,Object> 	metadataModel 	= new LinkedHashMap<String, Object>();
		WSEndpoint<?> 		endPoint 		= this.codec.getEndpoint();
		JAXBContextImpl 	context 	= (JAXBContextImpl)endPoint.getSEIModel().getJAXBContext();
		Map<String,Object>  service 		= new HashMap<String, Object>();
		metadataModel.put(endPoint.getServiceName().getLocalPart(), service );
		
		Module 				modules 		= endPoint.getContainer().getSPI(com.sun.xml.ws.api.server.Module.class);
		for(BoundEndpoint endPointObj : modules.getBoundEndpoints()){
			if(endPointObj.getEndpoint().getBinding().getBindingID() == JSONBindingID.JSON_BINDING){
				Map<String,Object>   portJSONMap 	= new HashMap<String, Object>();
				service.put(endPointObj.getEndpoint().getPortName().getLocalPart(), portJSONMap);
				
				SEIModel 	seiModel 		= endPointObj.getEndpoint().getSEIModel();
				for (WSDLBoundOperation operation : seiModel.getPort().getBinding().getBindingOperations()) {
					Map<String,Object>    operationMap = new HashMap<String, Object>();
					portJSONMap.put(operation.getName().getLocalPart(), operationMap );
					
					operationMap.put(operation.getOperation().getName().getLocalPart(), JSONHttpMetadataPublisher.getJSONAsMap(operation.getInParts(),
							context));
					
					operationMap.put(operation.getOperation().getOutput().getName(),JSONHttpMetadataPublisher.getJSONAsMap(operation.getOutParts(), context));
				}
			}
		}
		JSONReader 			reader 	= new JSONReader();
		Map<String,Object> 	doc 	= (Map<String,Object>)reader.read(WSJSONWriter.writeMetadata(metadataModel, this.codec.getCustomSerializer()));
		StringBuffer 		buffer 	= new StringBuffer();
		getJQTree(doc, buffer,0);
		endPointDocuments.put(this.codec.getEndpoint().getServiceName(),buffer.toString());
	}
	
	private void getJQTree(Map<String,Object> doc,StringBuffer buffer,int level){
		buffer.append('[');
		boolean isFirst = true;
		++level;
		for(String key : doc.keySet()){
			if(!isFirst)
				buffer.append(",");
			buffer.append("{\"text\": \""+key+"\",\"classes\":\"level"+ level+"\"");
			buffer.append(",\"expanded\": "+(level < 2)+"");
			Object value = doc.get(key);
			if(value != null && value instanceof Map){
				buffer.append(",\"children\":");
				getJQTree((Map<String, Object>) value,buffer,level);
			}
			buffer.append("}");
			isFirst = false;
		}
		buffer.append(']');
	}
	
	
	/**
	 * Output responder.
	 */
	public void doResponse(WSHTTPConnection ouStream) throws IOException {
		process();
		String portDocuments =  endPointDocuments.get(this.codec.getEndpoint().getServiceName());
		if(portDocuments != null){
			ouStream.getOutput().write(portDocuments.getBytes());
		}else{
			ouStream.getOutput().write(String.format("Unable to find default document for %s",
					this.codec.getEndpoint().getPortName()).getBytes());
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(HttpMetadataProvider o) {
		if(o.equals(this)){
			return 0;
		}else{
			return Integer.MAX_VALUE;
		}
	}
}