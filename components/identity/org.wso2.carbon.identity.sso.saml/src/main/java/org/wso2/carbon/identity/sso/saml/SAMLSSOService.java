/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.sso.saml;

import java.util.ArrayList;
import java.util.List;

import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.xml.XMLObject;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSORespDTO;
import org.wso2.carbon.identity.sso.saml.processors.AuthnRequestProcessor;
import org.wso2.carbon.identity.sso.saml.processors.LogoutRequestProcessor;
import org.wso2.carbon.identity.sso.saml.session.SSOSessionPersistenceManager;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.identity.sso.saml.validators.AuthnRequestValidator;

public class SAMLSSOService {

	/**
	 * Validates the SAMLRquest, the request can be the type AuthnRequest or
	 * LogoutRequest. The SigAlg and Signature parameter will be used only with
	 * the HTTP Redirect binding. With HTTP POST binding these values are null.
	 * If the user already having a SSO session then the Response
	 * will be returned if not only the validation results will be returned.
	 * 
	 * @param samlReq
	 * @param queryString
	 * @param sessionId
	 * @param rpSessionId
	 * @param authnMode
	 * @return
	 * @throws IdentityException
	 */
	public SAMLSSOReqValidationResponseDTO validateRequest(String samlReq, String queryString,
	                                                       String sessionId, String rpSessionId,
	                                                       String authnMode)
	                                                                        throws IdentityException {
		XMLObject request = SAMLSSOUtil.unmarshall(SAMLSSOUtil.decode(samlReq));
		if (request instanceof AuthnRequest) {
			AuthnRequestValidator authnRequestValidator =
			                                              new AuthnRequestValidator(
			                                                                        (AuthnRequest) request);
			SAMLSSOReqValidationResponseDTO validationResp = authnRequestValidator.validate();
			validationResp.setRequestMessageString(samlReq);
			validationResp.setQueryString(queryString);

			if (validationResp.isValid()) {
				SSOSessionPersistenceManager sessionPersistenceManager =
				                                                         SSOSessionPersistenceManager.getPersistenceManager();
				boolean isExistingSession = sessionPersistenceManager.isExistingSession(sessionId);
				if (authnMode.equals(SAMLSSOConstants.AuthnModes.OPENID) && !isExistingSession) {
					AuthnRequestProcessor authnRequestProcessor = new AuthnRequestProcessor();
					try {
						return authnRequestProcessor.process(validationResp, sessionId,
						                                     rpSessionId, authnMode);
					} catch (Exception e) {
						throw new IdentityException("Error processing the Authentication Request",
						                            e);
					}
				} else if (!isExistingSession && validationResp.isPassive()){
					List<String> statusCodes = new ArrayList<String>();
					statusCodes.add(SAMLSSOConstants.StatusCodes.NO_PASSIVE);
					statusCodes.add(SAMLSSOConstants.StatusCodes.IDENTITY_PROVIDER_ERROR);
					validationResp.setResponse(SAMLSSOUtil.buildErrorResponse(
					validationResp.getId(), statusCodes,"Cannot authenticate Subject in Passive Mode"));
				} else if (isExistingSession) { // now should send the Response
					AuthnRequestProcessor authnRequestProcessor = new AuthnRequestProcessor();
					try {
						return authnRequestProcessor.process(validationResp, sessionId,
						                                     rpSessionId, authnMode);
					} catch (Exception e) {
						throw new IdentityException("Error processing the Authentication Request",
						                            e);
					}
				}
			}
			validationResp.setRpSessionId(rpSessionId);
			return validationResp;
		} else if (request instanceof LogoutRequest) {
			LogoutRequestProcessor logoutReqProcessor = new LogoutRequestProcessor();
			SAMLSSOReqValidationResponseDTO validationResponseDTO =
			                                                        logoutReqProcessor.process((LogoutRequest) request,
			                                                                                   sessionId,
			                                                                                   queryString);
			return validationResponseDTO;
		}

		return null;
	}

	/**
	 * 
	 * @param authReqDTO
	 * @param sessionId
	 * @return
	 * @throws IdentityException
	 */
	public SAMLSSORespDTO authenticate(SAMLSSOAuthnReqDTO authReqDTO, String sessionId, boolean authenticated, String authMode)
	                                                                                   throws IdentityException {
		AuthnRequestProcessor authnRequestProcessor = new AuthnRequestProcessor();
		try {
			return authnRequestProcessor.process(authReqDTO, sessionId, authenticated, authMode);
		} catch (Exception e) {
			throw new IdentityException("Error when authenticating the users", e);
		}
	}

	/**
	 * Invalidates the SSO session for the given session ID
	 * @param sessionId
	 * @return
	 * @throws IdentityException
	 */
	public SAMLSSOReqValidationResponseDTO doSingleLogout(String sessionId)
	                                                                       throws IdentityException {
		LogoutRequestProcessor logoutReqProcessor = new LogoutRequestProcessor();
		SAMLSSOReqValidationResponseDTO validationResponseDTO =
		                                                        logoutReqProcessor.process(null,
		                                                                                   sessionId,
		                                                                                   null);
		return validationResponseDTO;
	}

    /**
     * Get the SSO session's timeout
     *
     * @return timeout for SSO session
     */
    public static int getSSOSessionTimeout() {
        if(IdentityUtil.getProperty(IdentityConstants.ServerConfig.SSO_SESSION_TIMEOUT) != null &&
                !IdentityUtil.getProperty(IdentityConstants.ServerConfig.SSO_SESSION_TIMEOUT).trim().equals("")){
            return Integer.parseInt(IdentityUtil.getProperty(IdentityConstants.ServerConfig.SSO_SESSION_TIMEOUT).trim());
        } else {
            return 36000;
        }
    }

    public static boolean isOpenIDLoginAccepted(){
        if(IdentityUtil.getProperty(IdentityConstants.ServerConfig.ACCEPT_OPENID_LOGIN) != null &&
                !IdentityUtil.getProperty(IdentityConstants.ServerConfig.ACCEPT_OPENID_LOGIN).trim().equals("")){
            return Boolean.parseBoolean(IdentityUtil.getProperty(IdentityConstants.ServerConfig.ACCEPT_OPENID_LOGIN).trim());
        } else {
            return false;
        }
    }

    public static boolean isSAMLSSOLoginAccepted(){
        if(IdentityUtil.getProperty(IdentityConstants.ServerConfig.ACCEPT_SAMLSSO_LOGIN) != null &&
                !IdentityUtil.getProperty(IdentityConstants.ServerConfig.ACCEPT_SAMLSSO_LOGIN).trim().equals("")){
            return Boolean.parseBoolean(IdentityUtil.getProperty(IdentityConstants.ServerConfig.ACCEPT_SAMLSSO_LOGIN).trim());
        } else {
            return false;
        }
    }

}
