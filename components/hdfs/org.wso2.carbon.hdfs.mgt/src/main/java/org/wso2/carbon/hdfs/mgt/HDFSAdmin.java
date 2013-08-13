/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.hdfs.mgt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.hdfs.dataaccess.DataAccessService;

/**
 * HDFS file system access service
 */
public class HDFSAdmin extends AbstractAdmin {

	// private Configuration configuration = new Configuration(false);
	// set FS default user home directory.
	private static final String USER_HOME = "/user/";
	private static Log log = LogFactory.getLog(HDFSAdminComponentManager.class);

	/**
	 * Mgt service return file and folder list of the give HDFS path
	 * 
	 * @param fsObjectPath
	 *            file system path which user need info about files and folders
	 * @return list with files and folders in the given path
	 * @throws HDFSServerManagementException
	 */
	public FolderInformation[] getCurrentUserFSObjects(String fsObjectPath)
			throws HDFSServerManagementException {

		if (fsObjectPath == null) {
			fsObjectPath = USER_HOME + fsObjectPath;
		}
		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();

		FileSystem hdfsFS = null;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
		} catch (IOException e) {
			String msg = "Error occurred while mouting the file system";
			handleException(msg, e);
		}
		FileStatus[] fileStatusList = null;

		List<FolderInformation> folderInfo = new ArrayList<FolderInformation>();
		try {
			if (hdfsFS != null && hdfsFS.exists(new Path(fsObjectPath))) {
				if (hdfsFS != null) {
					//List the statuses of the files/directories in the given path if the path is a directory.
					fileStatusList = hdfsFS.listStatus(new Path(fsObjectPath));
				}
				if (fileStatusList != null) {
					for (FileStatus fileStatus : fileStatusList) {
						FolderInformation folder = new FolderInformation();
						folder.setFolder(fileStatus.isDir());
						folder.setName(fileStatus.getPath().getName());
						folder.setFolderPath(fileStatus.getPath().toUri()
								.getPath());
						folder.setOwner(fileStatus.getOwner());
						folder.setGroup(fileStatus.getGroup());
						folder.setPermissions(fileStatus.getPermission()
								.toString());
						folderInfo.add(folder);
					}
					return folderInfo.toArray(new FolderInformation[folderInfo
							.size()]);
				}
			}
		} catch (IOException e) {
			 String msg = "Error occurred while retrieving folder information";
			handleException(msg, e);
		} 
		return null;

	}

	public void copy(String srcPath, String dstPath)
			throws HDFSServerManagementException {
		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();

		FileSystem hdfsFS = null;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
		} catch (IOException e) {
			String msg = "Error occurred while mouting the file system";
			handleException(msg, e);
		}

		Path[] srcs = new Path[0];
		if (hdfsFS != null) {
			try {
				srcs = FileUtil
						.stat2Paths(hdfsFS.globStatus(new Path(srcPath)),
								new Path(srcPath));
			} catch (IOException e) {
				String msg = "Error occurred while trying to copy file.";
				handleException(msg, e);
			}
		}
		try {
			if (srcs.length > 1 && !hdfsFS.isDirectory(new Path(dstPath))) {
				throw new IOException("When copying multiple files, "
						+ "destination should be a directory.");
			}
		} catch (IOException e) {
			String msg = "Error occurred while trying to copy file.";
			handleException(msg, e);
		}
		Configuration configuration = new Configuration();
		configuration.set("io.file.buffer.size", Integer.toString(4096));
		for (int i = 0; i < srcs.length; i++) {
			try {
				FileUtil.copy(hdfsFS, srcs[i], hdfsFS, new Path(dstPath),
						false, configuration);
			} catch (IOException e) {
				String msg = "Error occurred while trying to copy file.";
				handleException(msg, e);
			}
		}
	}

	/**
	 * Delete the HDFS file in the given path
	 * 
	 * @param filePath
	 *            File path for the file to be deleted
	 * @return return true if the file deletetion is a success
	 */
	public boolean deleteFile(String filePath)
			throws HDFSServerManagementException {

		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();

		FileSystem hdfsFS = null;
		Path path =  new Path(filePath);
		boolean folderExists=true;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
		} catch (IOException e) {
			String msg = "Error occurred while trying to delete file. "+ filePath;
			handleException(msg, e);
		}
		try {
			/**
			 * HDFS delete with recursive delete off
			 */
			if(hdfsFS != null && hdfsFS.exists(path)){
				return hdfsFS.delete(path, false);
			}
			else{
				folderExists = false;
			}
		} catch (IOException e) {
			String msg = "Error occurred while trying to delete file.";
			handleException(msg, e);
		}
		
		handleItemExistState(folderExists, false, true);
		
		return false;
	}
	
	/**
	 *  Handles throwing an exception on the items existance.
	 * @param ItemExists
	 * 			holds whether the item exists
	 * @param throwExceptionWhenItemExists
	 * 			holds whether to show the error message when the item exists or to show it when the item is not existing
	 * @param isFolder
	 * 			holder if it is a folder
	 * @throws HDFSOperationException
	 */
	protected void handleItemExistState(boolean ItemExists, boolean throwExceptionWhenItemExists, boolean isFolder) throws HDFSOperationException
	{
		String msg= null;
		String prefix = "File";
		if(isFolder)
		{
			prefix = "Folder";
		}
		if(throwExceptionWhenItemExists){
			msg = prefix + " already exists";
			if(ItemExists){
				throw new HDFSOperationException(msg, log);
			}
		}else{
			msg = prefix + " does not exist";
			if(!ItemExists){
				throw new HDFSOperationException(msg, log);
			}
		}
	}

	/**
	 * Delete the HDFS folder in the given path
	 * 
	 * @param folderPath
	 *            Path Folder path for the folder to be deleted
	 * @return return true if folder deletion is a success
	 * @throws
	 */
	public boolean deleteFolder(String folderPath)
			throws HDFSServerManagementException {

		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();

		FileSystem hdfsFS = null;
		boolean isFolderExist = false; 
		Path path = new Path(folderPath);
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
		} catch (IOException e) {
			String msg = "Error occurred while trying to mount file system.";
			handleException(msg, e);
		}

		try {
			/**
			 * HDFS delete with recursive delete on to delete folder and the
			 * content
			 */
			if(hdfsFS != null && hdfsFS.exists(path)){
				isFolderExist = true;
				return hdfsFS.delete(path, true);
			}
			
		} catch (IOException e) {
			String msg = "Error occurred while trying to delete folder.";
			handleException(msg, e);
		}

		handleItemExistState(isFolderExist, false, true);
		return false;
	}

	/**
	 * Rename file or a folder using source and the destination of the give FS
	 * Object
	 * 
	 * @param srcPath
	 *            Current path and the file name of the file to be renamed
	 * @param dstPath
	 *            new pathe and the file name
	 * @return success if rename is successful
	 * @throws HDFSServerManagementException
	 */

	public boolean renameFile(String srcPath, String dstPath)
			throws HDFSServerManagementException {

		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();
		FileSystem hdfsFS = null;
		Path src = new Path(srcPath);
		Path dest = new Path(dstPath);
		boolean fileExists = false;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
		} catch (IOException e) {
			String msg = "Error occurred while trying to mount file system.";
			handleException(msg, e);
		}
		try {
			if (hdfsFS != null && !hdfsFS.exists(dest)) {
				return hdfsFS.rename(src, dest);
			} else {
			    fileExists = true;
			}
		} catch (IOException e) {
			String msg = "Error occurred while trying to rename file.";
			handleException(msg, e);
		}
		handleItemExistState(fileExists, true, false);
		return false;
	}

	/**
	 * Rename file or a folder using source and the destination of the give FS
	 * Object
	 * 
	 * @param srcPath
	 *            Current path and the file name of the file to be renamed
	 * @param dstPath
	 *            new pathe and the file name
	 * @return success if rename is successful
	 * @throws HDFSServerManagementException
	 */

	public boolean renameFolder(String srcPath, String dstPath)
			throws HDFSServerManagementException {

		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();
		FileSystem hdfsFS = null;
		boolean isFolderExists = false;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
		} catch (IOException e) {
			String msg = "Error occurred while trying to mount file system.";
			handleException(msg, e);
		}
		try {
			if (hdfsFS != null && !hdfsFS.exists(new Path(dstPath))) {
				return hdfsFS.rename(new Path(srcPath), new Path(dstPath));
			} else {
				isFolderExists = true;
			}
		} catch (IOException e) {
			String msg = "Error occurred while trying to rename folder.";
			handleException(msg, e);
		}
		handleItemExistState(isFolderExists, true, true);
		return false;
	}

	public boolean moveFile(String srcPath, String dstPath)
			throws HDFSServerManagementException {
		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();
		FileSystem hdfsFS = null;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
		} catch (IOException e) {
			String msg = "Error occurred while trying to mount file system.";
			handleException(msg, e);
		}

		try {
			if(hdfsFS != null)
			{
				return hdfsFS.rename(new Path(srcPath), new Path(dstPath));
			}

		} catch (IOException e) {
			String msg = "Error occurred while trying to move file.";
			handleException(msg, e);
		}

		return false;
	}

	public boolean makeDirectory(String folderPath)
			throws HDFSServerManagementException {
		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();
		FileSystem hdfsFS = null;
		boolean folderExists = false;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
			
			if (hdfsFS != null && !hdfsFS.exists(new Path(folderPath))) {
				return hdfsFS.mkdirs(new Path(folderPath));
			} else {
				folderExists = true;
			}
		}catch (IOException e) {
			String msg = "Error occurred while trying to make a directory.";
			handleException(msg, e);
		}
		 handleItemExistState(folderExists, true, true);
		return false;
	}

	// public boolean makeSymLink(String target, String link, boolean
	// createParent) throws HDFSServerManagementException {
	// DataAccessService dataAccessService =
	// HDFSAdminComponentManager.getInstance().getDataAccessService();
	// FileSystem hdfsFS = null;
	// try {
	// //hdfsFS = dataAccessService.mountFileSystem(getClusterConfiguration());
	// hdfsFS = dataAccessService.mountCurrentUserFileSystem();
	// return hdfsFS.create(new Path(folderPath));
	//
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// return false;
	// }


	

	public String getPermission(String fsPath)
			throws HDFSServerManagementException {
		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();
		FileSystem hdfsFS = null;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
			if(hdfsFS != null)
			{
				return hdfsFS.getFileStatus(new Path(fsPath)).getPermission()
						.toString();
			}

		} catch (IOException e) {
			String msg = "Error occurred while trying to mount file system.";
			handleException(msg, e);
		}

		return null;
	}

	public void setPermission(String fsPath, String fsPermission)
			throws HDFSServerManagementException {
		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();
		FileSystem hdfsFS = null;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
			if(hdfsFS != null){
			hdfsFS.setPermission(new Path(fsPath), new FsPermission(
					fsPermission));
			}

		} catch (IOException e) {
			String msg = "Error occurred while trying to mount file system.";
			handleException(msg, e);
		}
	}

	public void setGroup(String fsPath, String group)
			throws HDFSServerManagementException {
		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();

		FileSystem hdfsFS = null;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
			if(hdfsFS != null){
			hdfsFS.setOwner(new Path(fsPath), null, group); 
			}// TO DO: validate
															// the group / role

		} catch (IOException e) {
			String msg = "Error occurred while trying to mount file system.";
			handleException(msg, e);
		}

	}

	public void setOwner(String fsPath, String owner)
			throws HDFSServerManagementException {
		DataAccessService dataAccessService = HDFSAdminComponentManager
				.getInstance().getDataAccessService();
		FileSystem hdfsFS = null;
		try {
			hdfsFS = dataAccessService.mountCurrentUserFileSystem();
			if(hdfsFS != null){
			hdfsFS.setOwner(new Path(fsPath), owner, null);
			}// TO DO: validate
															// the group / role

		} catch (IOException e) {
			String msg = "Error occurred while trying to mount file system.";
			handleException(msg, e);
		}
	}
	
	 protected void handleException(String msg, Exception e) throws HDFSServerManagementException {
	        log.error(msg, e);
	        throw new HDFSServerManagementException(msg, log);
	    }

}
