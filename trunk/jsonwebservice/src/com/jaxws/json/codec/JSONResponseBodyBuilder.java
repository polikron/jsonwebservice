package com.jaxws.json.codec;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.sun.istack.NotNull;
import com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.message.jaxb.JAXBMessage;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.model.ParameterImpl;
import com.sun.xml.ws.model.WrapperParameter;

public class JSONResponseBodyBuilder extends MessageBodyBuilder{
	private JSONCodec codec;
	public JSONResponseBodyBuilder(@NotNull JSONCodec codec) {
		super(codec);
		this.codec = codec;
	}
	
	/**
	 * 	 * Response used back as Request
	 */
	public Message createMessage(JavaMethodImpl methodImpl, 
								Map<String, Object> responseJSONObject,
								JAXBContextImpl context) {
		Pattern listMapKey		= JSONCodec.getListMapKey(methodImpl);
		Pattern listMapValue	= JSONCodec.getListMapValue(methodImpl);
		
		Collection<Object> parameterObjects = readParameterAsObjects(
				methodImpl.getResponseParameters(),
				responseJSONObject,context,listMapKey,listMapValue).values();
		ParameterImpl responseParameter = methodImpl.getResponseParameters().get(0);
		
		if(parameterObjects.size() ==0){
			//VOID response
			return JAXBMessage.create(responseParameter.getBridge(), null, codec.soapVersion);
		}
		
		if(responseParameter instanceof WrapperParameter &&
					((WrapperParameter)responseParameter).getTypeReference().type!= com.sun.xml.bind.api.CompositeStructure.class){
			WrapperParameter responseWarper = (WrapperParameter)responseParameter;
			return JAXBMessage.create(responseWarper.getWrapperChildren().get(0).getBridge(),
					responseWarper.getWrapperChildren().get(0), codec.soapVersion);
		}else{
			return JAXBMessage.create(responseParameter.getBridge(), parameterObjects.toArray()[0], codec.soapVersion);
		}
	}

	/**
	 * This is normal standard call
	 * @throws XMLStreamException 
	 * @throws JAXBException 
	 */
	public Map<String,Object> createMap(JavaMethodImpl methodImpl,Message message) throws JAXBException, XMLStreamException{
		
		Pattern listMapKey		= JSONCodec.getListMapKey(methodImpl);
		Pattern listMapValue	= JSONCodec.getListMapValue(methodImpl);
		//Encode as Response
		Map<String,Object> parameterObjects = readParameterAsObjects(
											methodImpl.getResponseParameters(),
											null,null,listMapKey,listMapValue);
		assert parameterObjects.size() <= 1;
		HashMap<String, Object> parameters = new LinkedHashMap<String, Object>();
		if(!parameterObjects.keySet().isEmpty()){	
			parameters.put(parameterObjects.keySet().toArray()[0].toString(), 
					getResponseBuilder(methodImpl.getResponseParameters())
					.readResponse(message, parameterObjects.values().toArray()));
		}
		return parameters;
	}
	
}
