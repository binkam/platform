/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.transport.adaptor.manager.core.internal;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.transport.adaptor.core.TransportAdaptorDto;
import org.wso2.carbon.transport.adaptor.core.TransportAdaptorService;
import org.wso2.carbon.transport.adaptor.manager.core.TransportAdaptorConfiguration;
import org.wso2.carbon.transport.adaptor.manager.core.TransportAdaptorFile;
import org.wso2.carbon.transport.adaptor.manager.core.TransportManagerService;
import org.wso2.carbon.transport.adaptor.manager.core.exception.TransportManagerConfigurationException;
import org.wso2.carbon.transport.adaptor.manager.core.internal.config.TransportConfigurationFilesystemInvoker;
import org.wso2.carbon.transport.adaptor.manager.core.internal.config.TransportConfigurationHelper;
import org.wso2.carbon.transport.adaptor.manager.core.internal.util.TMConstants;
import org.wso2.carbon.transport.adaptor.manager.core.internal.util.TransportAdaptorHolder;
import org.wso2.carbon.transport.adaptor.manager.core.internal.util.TransportAdaptorInfo;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * carbon implementation of the transport manager.
 */
public class CarbonTransportManagerService implements TransportManagerService {
    private static final Log log = LogFactory.getLog(CarbonTransportManagerService.class);

    /**
     * transport configuration map to keep the transport configuration details
     */

    private Map<Integer, Map<String, TransportAdaptorConfiguration>> tenantSpecificTransportConfigurationMap;
    private Map<Integer, Map<String, TransportAdaptorInfo>> tenantSpecificInputTransportInfoMap;
    private Map<Integer, Map<String, TransportAdaptorInfo>> tenantSpecificOutputTransportInfoMap;

    private Map<Integer, List<TransportAdaptorFile>> transportAdaptorFileMap;


    public CarbonTransportManagerService() {
        tenantSpecificTransportConfigurationMap = new ConcurrentHashMap<Integer, Map<String, TransportAdaptorConfiguration>>();
        transportAdaptorFileMap = new ConcurrentHashMap<Integer, List<TransportAdaptorFile>>();
        tenantSpecificInputTransportInfoMap = new HashMap<Integer, Map<String, TransportAdaptorInfo>>();
        tenantSpecificOutputTransportInfoMap = new HashMap<Integer, Map<String, TransportAdaptorInfo>>();
    }

    public void addFileConfiguration(int tenantId, String transportAdaptorName, String filePath, boolean flag) {

        List<TransportAdaptorFile> transportAdaptorFileList = transportAdaptorFileMap.get(tenantId);

        if (transportAdaptorFileList == null) {
            transportAdaptorFileList = new ArrayList<TransportAdaptorFile>();

            TransportAdaptorFile transportAdaptorFile = new TransportAdaptorFile();
            transportAdaptorFile.setFilePath(filePath);
            transportAdaptorFile.setTransportAdaptorName(transportAdaptorName);
            transportAdaptorFile.setSuccess(flag);
            transportAdaptorFileList.add(transportAdaptorFile);

            transportAdaptorFileMap.put(tenantId, transportAdaptorFileList);
        } else {
            TransportAdaptorFile transportAdaptorFile = new TransportAdaptorFile();
            transportAdaptorFile.setFilePath(filePath);
            transportAdaptorFile.setTransportAdaptorName(transportAdaptorName);
            transportAdaptorFile.setSuccess(flag);
            transportAdaptorFileList.add(transportAdaptorFile);
            transportAdaptorFileMap.put(tenantId, transportAdaptorFileList);

        }

    }

    public void editTransportConfigurationFile(String transportAdaptorConfiguration, String transportAdaptorName, AxisConfiguration axisConfiguration) throws TransportManagerConfigurationException {
        int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();

        try {
            OMElement omElement = AXIOMUtil.stringToOM(transportAdaptorConfiguration);
            omElement.toString();
            if (TransportConfigurationHelper.validateTransportAdaptorConfiguration(tenantId, TransportConfigurationHelper.fromOM(omElement))) {
                String pathInFileSystem = getFilePath(tenantId, transportAdaptorName);
                removeTransportConfiguration(transportAdaptorName, axisConfiguration);
                TransportConfigurationFilesystemInvoker.saveConfigurationToFileSystem(omElement, transportAdaptorName, pathInFileSystem, axisConfiguration);
            }
        } catch (XMLStreamException e) {
            log.error("Error while creating the xml object");
        }
    }


    public void saveTransportConfiguration(TransportAdaptorConfiguration transportAdaptorConfiguration,
                                           AxisConfiguration axisConfiguration) throws TransportManagerConfigurationException {

        String transportName = transportAdaptorConfiguration.getName();
        OMElement omElement = TransportConfigurationHelper.transportAdaptorConfigurationToOM(transportAdaptorConfiguration);
        int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();

        if (TransportConfigurationHelper.validateTransportAdaptorConfiguration(tenantId, TransportConfigurationHelper.fromOM(omElement))) {
            File directory = new File(axisConfiguration.getRepository().getPath());
            if (!directory.exists()) {
                if (directory.mkdir()) {
                    throw new TransportManagerConfigurationException("Cannot create directory to add tenant specific transport adaptor :" + transportName);
                }
            }
            directory = new File(directory.getAbsolutePath() + File.separator + TMConstants.TM_ELE_DIRECTORY);
            if (!directory.exists()) {
                if (!directory.mkdir()) {
                    throw new TransportManagerConfigurationException("Cannot create directory " + TMConstants.TM_ELE_DIRECTORY + " to add tenant specific transport adaptor :" + transportName);
                }
            }

            String pathInFileSystem = directory.getAbsolutePath() + File.separator + transportName + ".xml";
            TransportConfigurationFilesystemInvoker.saveConfigurationToFileSystem(omElement, transportName, pathInFileSystem, axisConfiguration);
        }
    }


    public void removeTransportConfiguration(String transportAdaptorName,
                                             AxisConfiguration axisConfiguration) throws TransportManagerConfigurationException {
        int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();

        List<TransportAdaptorFile> transportAdaptorFileList = transportAdaptorFileMap.get(tenantId);
        Iterator<TransportAdaptorFile> transportAdaptorFileIterator = transportAdaptorFileList.iterator();
        while (transportAdaptorFileIterator.hasNext()) {

            TransportAdaptorFile transportAdaptorFile = transportAdaptorFileIterator.next();
            if ((transportAdaptorFile.getTransportAdaptorName().equals(transportAdaptorName))) {

                String filePath = transportAdaptorFile.getFilePath();
                File file = new File(filePath);
                if (file.exists()) {
                    boolean fileDeleted = file.delete();
                    if (!fileDeleted) {
                        log.error("Could not delete " + filePath);
                    } else {
                        log.info(filePath + " is deleted from the file system");
                        TransportConfigurationFilesystemInvoker.executeUnDeploy(filePath, axisConfiguration);

                    }
                    break;
                }

            }
        }

    }

    public void removeTransportAdaptorFile(String filePath, AxisConfiguration axisConfiguration) throws TransportManagerConfigurationException {

        File file = new File(filePath);
        if (file.exists()) {
            boolean fileDeleted = file.delete();
            if (!fileDeleted) {
                log.error("Could not delete " + filePath);
            } else {

                log.info(filePath + " is deleted from the file system");
                TransportConfigurationFilesystemInvoker.executeUnDeploy(filePath, axisConfiguration);
            }
        }

    }

    @Override
    public String getTransportConfigurationFile(String transportAdaptorName, AxisConfiguration axisConfiguration) throws TransportManagerConfigurationException {
        int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();

        OMElement transportAdaptorOMElement = null;
        List<TransportAdaptorFile> transportAdaptorFileList = transportAdaptorFileMap.get(tenantId);
        Iterator<TransportAdaptorFile> transportAdaptorFileIterator = transportAdaptorFileList.iterator();
        while (transportAdaptorFileIterator.hasNext()) {

            TransportAdaptorFile transportAdaptorFile = transportAdaptorFileIterator.next();
            if ((transportAdaptorFile.getTransportAdaptorName().equals(transportAdaptorName))) {

                String filePath = transportAdaptorFile.getFilePath();
                File file = new File(transportAdaptorFile.getFilePath());
                if (file.exists()) {
                    transportAdaptorOMElement = getTransportOMElement(filePath, file);

                }

            }
        }
        return transportAdaptorOMElement.toString();
    }


    public List<TransportAdaptorConfiguration> getAllTransportConfigurations(AxisConfiguration axisConfiguration) {
        List<TransportAdaptorConfiguration> transportAdaptorConfigurations = new ArrayList<TransportAdaptorConfiguration>();
        int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();
        if (tenantSpecificTransportConfigurationMap.get(tenantId) != null) {
            for (TransportAdaptorConfiguration transportAdaptorConfiguration : tenantSpecificTransportConfigurationMap.get(
                    tenantId).values()) {
                transportAdaptorConfigurations.add(transportAdaptorConfiguration);
            }
        }
        return transportAdaptorConfigurations;
    }

    public List<TransportAdaptorFile> getUnDeployedFiles(AxisConfiguration axisConfiguration) {

        List<TransportAdaptorFile> unDeployedTransportAdaptorFileList = new ArrayList<TransportAdaptorFile>();
        int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();
        if (transportAdaptorFileMap.get(tenantId) != null) {
            for (TransportAdaptorFile transportAdaptorFile : transportAdaptorFileMap.get(tenantId)) {
                if (!transportAdaptorFile.isSuccess()) {
                    unDeployedTransportAdaptorFileList.add(transportAdaptorFile);
                }
            }
        }
        return unDeployedTransportAdaptorFileList;
    }


    @Override
    public TransportAdaptorConfiguration getTransportConfiguration(String name, AxisConfiguration axisConfiguration) throws TransportManagerConfigurationException {

        int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();

        if (tenantSpecificTransportConfigurationMap.get(tenantId) == null) {
            throw new TransportManagerConfigurationException("There is no any configuration exists for " + tenantId);
        }
        return tenantSpecificTransportConfigurationMap.get(tenantId).get(name);
    }

    @Override
    public Map<String, String> getInputTransportAdaptorConfiguration(String transportAdaptorName, int tenantId) {

        Map<String, TransportAdaptorConfiguration> transportAdaptors = tenantSpecificTransportConfigurationMap.get(tenantId);
        Map<String, String> inProperties = null;
        if (transportAdaptors.containsKey(transportAdaptorName)) {
            TransportAdaptorConfiguration transportAdaptorConfiguration = transportAdaptors.get(transportAdaptorName);


            if (!transportAdaptorConfiguration.getInputAdaptorProperties().isEmpty()) {
                inProperties = transportAdaptorConfiguration.getInputAdaptorProperties();
            }

            if (!transportAdaptorConfiguration.getCommonAdaptorProperties().isEmpty()) {

                if (inProperties != null) {
                    Iterator commonAdaptorPropertyIterator = transportAdaptorConfiguration.getCommonAdaptorProperties().entrySet().iterator();
                    while (commonAdaptorPropertyIterator.hasNext()) {
                        Map.Entry thisEntry = (Map.Entry) commonAdaptorPropertyIterator.next();
                        inProperties.put(thisEntry.getKey().toString(), thisEntry.getValue().toString());
                    }
                } else {
                    inProperties = transportAdaptorConfiguration.getCommonAdaptorProperties();
                }
            }


        }
        return inProperties;
    }


    @Override
    public Map<String, String> getOutputTransportAdaptorConfiguration(String transportAdaptorName, int tenantId) {

        Map<String, TransportAdaptorConfiguration> transportAdaptors = tenantSpecificTransportConfigurationMap.get(tenantId);
        Map<String, String> outProperties = null;
        if (transportAdaptors.containsKey(transportAdaptorName)) {
            TransportAdaptorConfiguration transportAdaptorConfiguration = transportAdaptors.get(transportAdaptorName);


            if (!transportAdaptorConfiguration.getOutputAdaptorProperties().isEmpty()) {
                outProperties = transportAdaptorConfiguration.getOutputAdaptorProperties();
            }

            if (!transportAdaptorConfiguration.getCommonAdaptorProperties().isEmpty()) {

                if (outProperties != null) {
                    Iterator commonAdaptorPropertyIterator = transportAdaptorConfiguration.getCommonAdaptorProperties().entrySet().iterator();
                    while (commonAdaptorPropertyIterator.hasNext()) {
                        Map.Entry thisEntry = (Map.Entry) commonAdaptorPropertyIterator.next();
                        outProperties.put(thisEntry.getKey().toString(), thisEntry.getValue().toString());
                    }
                } else {
                    outProperties = transportAdaptorConfiguration.getCommonAdaptorProperties();
                }
            }


        }
        return outProperties;
    }

    @Override
    public List<TransportAdaptorInfo> getInputTransportAdaptorInfo(int tenantId) {

        Map<String, TransportAdaptorInfo> inputTransportAdaptorInfoMap = tenantSpecificInputTransportInfoMap.get(tenantId);

        if (inputTransportAdaptorInfoMap != null) {
            return (List) inputTransportAdaptorInfoMap.values();
        }
        return null;

    }

    @Override
    public List<TransportAdaptorInfo> getOutputTransportAdaptorInfo(int tenantId) {

        Map<String, TransportAdaptorInfo> outputTransportAdaptorInfoMap = tenantSpecificOutputTransportInfoMap.get(tenantId);
        if (outputTransportAdaptorInfoMap != null) {
            return (List) outputTransportAdaptorInfoMap.values();
        }
        return null;

    }


    private void addToTenantSpecificTransportAdaptorInfoMap(int tenantId, TransportAdaptorConfiguration transportAdaptorConfiguration) throws TransportManagerConfigurationException {

        TransportAdaptorService transportAdaptorService = TransportAdaptorHolder.getInstance().getTransportAdaptorService();
        TransportAdaptorDto transportAdaptorDto = transportAdaptorService.getTransportAdaptorDto(transportAdaptorConfiguration.getType());


        if (transportAdaptorDto.getTransportAdaptorType().equals(TransportAdaptorDto.TransportAdaptorType.IN)) {
            addToInputTransportInfoMap(tenantId, transportAdaptorConfiguration);

        } else if (transportAdaptorDto.getTransportAdaptorType().equals(TransportAdaptorDto.TransportAdaptorType.OUT)) {
            addToOutputTransportInfoMap(tenantId, transportAdaptorConfiguration);


        } else if (transportAdaptorDto.getTransportAdaptorType().equals(TransportAdaptorDto.TransportAdaptorType.INOUT)) {
            addToInputTransportInfoMap(tenantId, transportAdaptorConfiguration);
            addToOutputTransportInfoMap(tenantId, transportAdaptorConfiguration);

        }
    }

    private void addToInputTransportInfoMap(int tenantId, TransportAdaptorConfiguration transportAdaptorConfiguration) {


        TransportAdaptorInfo transportAdaptorInfo = new TransportAdaptorInfo();
        transportAdaptorInfo.setTransportAdaptorName(transportAdaptorConfiguration.getName());
        transportAdaptorInfo.setTransportAdaptorType(transportAdaptorConfiguration.getType());

        Map<String, TransportAdaptorInfo> transportAdaptorInfoMap = tenantSpecificInputTransportInfoMap.get(tenantId);

        if (transportAdaptorInfoMap != null) {

            transportAdaptorInfoMap.put(transportAdaptorConfiguration.getName(), transportAdaptorInfo);
        } else {
            transportAdaptorInfoMap = new HashMap<String, TransportAdaptorInfo>();
            transportAdaptorInfoMap.put(transportAdaptorConfiguration.getName(), transportAdaptorInfo);
        }

        tenantSpecificInputTransportInfoMap.put(tenantId, transportAdaptorInfoMap);


    }

    private void addToOutputTransportInfoMap(int tenantId, TransportAdaptorConfiguration transportAdaptorConfiguration) {

        TransportAdaptorInfo transportAdaptorInfo = new TransportAdaptorInfo();
        transportAdaptorInfo.setTransportAdaptorName(transportAdaptorConfiguration.getName());
        transportAdaptorInfo.setTransportAdaptorType(transportAdaptorConfiguration.getType());

        Map<String, TransportAdaptorInfo> transportAdaptorInfoMap = tenantSpecificOutputTransportInfoMap.get(tenantId);

        if (transportAdaptorInfoMap != null) {

            transportAdaptorInfoMap.put(transportAdaptorConfiguration.getName(), transportAdaptorInfo);
        } else {
            transportAdaptorInfoMap = new HashMap<String, TransportAdaptorInfo>();
            transportAdaptorInfoMap.put(transportAdaptorConfiguration.getName(), transportAdaptorInfo);
        }

        tenantSpecificOutputTransportInfoMap.put(tenantId, transportAdaptorInfoMap);

    }


    private void removeFromTenantSpecificTransportAdaptorInfoMap(int tenantId, String transportAdaptorName) {

        Map<String, TransportAdaptorInfo> inputTransportAdaptorInfoMap = tenantSpecificInputTransportInfoMap.get(tenantId);
        Map<String, TransportAdaptorInfo> outputTransportAdaptorInfoMap = tenantSpecificOutputTransportInfoMap.get(tenantId);

        if (inputTransportAdaptorInfoMap != null && inputTransportAdaptorInfoMap.containsKey(transportAdaptorName)) {
            inputTransportAdaptorInfoMap.remove(transportAdaptorName);
        }

        if (outputTransportAdaptorInfoMap != null && outputTransportAdaptorInfoMap.containsKey(transportAdaptorName)) {
            outputTransportAdaptorInfoMap.remove(transportAdaptorName);
        }


    }


    public boolean checkAdaptorValidity(int tenantId, String transportAdaptorName) {

        if (transportAdaptorFileMap.size() > 0) {
            List<TransportAdaptorFile> transportAdaptorFileList = transportAdaptorFileMap.get(tenantId);

            Iterator<TransportAdaptorFile> transportAdaptorFileIterator = transportAdaptorFileList.iterator();
            while (transportAdaptorFileIterator.hasNext()) {

                TransportAdaptorFile transportAdaptorFile = transportAdaptorFileIterator.next();
                if ((transportAdaptorFile.getTransportAdaptorName().equals(transportAdaptorName)) && (transportAdaptorFile.isSuccess())) {

                    log.error("Transport adaptor " + transportAdaptorName + " is already registered with this tenant");
                    return false;
                }
            }
        }
        return true;

    }


    private OMElement getTransportOMElement(String path, File transportAdaptorFile) throws TransportManagerConfigurationException {
        OMElement transportAdaptorElement;
        BufferedInputStream inputStream = null;
        try {

            inputStream = new BufferedInputStream(new FileInputStream(transportAdaptorFile));
            XMLStreamReader parser = XMLInputFactory.newInstance().
                    createXMLStreamReader(inputStream);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            transportAdaptorElement = builder.getDocumentElement();
            transportAdaptorElement.build();

        } catch (Exception e) {
            String errorMessage = " .xml file cannot be found in the path : " + path;
            log.error(errorMessage, e);
            throw new TransportManagerConfigurationException(errorMessage, e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                String errorMessage = "Can not close the input stream";
                log.error(errorMessage, e);
            }
        }
        return transportAdaptorElement;
    }

    public void removeTransportConfigurationFromMap(String filePath, int tenantId) {
        List<TransportAdaptorFile> transportAdaptorFileList = transportAdaptorFileMap.get(tenantId);

        Iterator<TransportAdaptorFile> transportAdaptorFileIterator = transportAdaptorFileList.iterator();
        while (transportAdaptorFileIterator.hasNext()) {

            TransportAdaptorFile transportAdaptorFile = transportAdaptorFileIterator.next();
            if ((transportAdaptorFile.getFilePath().equals(filePath))) {
                if (transportAdaptorFile.isSuccess()) {
                    String transportAdaptorName = transportAdaptorFile.getTransportAdaptorName();
                    tenantSpecificTransportConfigurationMap.get(tenantId).remove(transportAdaptorName);
                    removeFromTenantSpecificTransportAdaptorInfoMap(tenantId, transportAdaptorName);
                }
                transportAdaptorFileList.remove(transportAdaptorFile);
                return;
            }
        }


    }


    private String getFilePath(int tenantId, String transportAdaptorName) {

        if (transportAdaptorFileMap.size() > 0) {
            List<TransportAdaptorFile> transportAdaptorFileList = transportAdaptorFileMap.get(tenantId);

            Iterator<TransportAdaptorFile> transportAdaptorFileIterator = transportAdaptorFileList.iterator();
            while (transportAdaptorFileIterator.hasNext()) {

                TransportAdaptorFile transportAdaptorFile = transportAdaptorFileIterator.next();
                if ((transportAdaptorFile.getTransportAdaptorName().equals(transportAdaptorName))) {
                    return transportAdaptorFile.getFilePath();
                }
            }
        }
        return null;
    }


    public void addTransportConfigurationForTenant(
            int tenantId, TransportAdaptorConfiguration transportAdaptorConfiguration) throws TransportManagerConfigurationException {
        Map<String, TransportAdaptorConfiguration> transportConfigurationMap
                = tenantSpecificTransportConfigurationMap.get(tenantId);
        if (transportConfigurationMap == null) {
            transportConfigurationMap = new ConcurrentHashMap<String, TransportAdaptorConfiguration>();
            transportConfigurationMap.put(transportAdaptorConfiguration.getName(), transportAdaptorConfiguration);
            tenantSpecificTransportConfigurationMap.put(tenantId, transportConfigurationMap);
        } else {
            transportConfigurationMap.put(transportAdaptorConfiguration.getName(), transportAdaptorConfiguration);
        }

        addToTenantSpecificTransportAdaptorInfoMap(tenantId, transportAdaptorConfiguration);

    }

}
