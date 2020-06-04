/*
 * Copyright (c) 2020 CertifAI
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.certifai.selector;

import ai.certifai.data.type.image.ImageFileType;
import ai.certifai.database.portfolio.PortfolioVerticle;
import ai.certifai.database.project.ProjectVerticle;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SelectorHandler {

    //key: (Integer) Project ID
    //value: (String) Project Name
    @Getter private static Map projectIDNameDict;

    //projectUUIDNameDict opposite
    //key: (String) Project Name
    //value: (Integer) Project ID
    @Getter private static Map projectNameIDDict;

    private static AtomicInteger projectIDGenerator;
    private static AtomicInteger uuidGenerator;

    @Getter private static String projectNameBuffer;

    private static boolean isWindowOpen = false;
    @Getter private static String currentWindowSelection;//FILE FOLDER
    private static boolean isDatabaseUpdating = false;

    @Getter private static List<File> fileHolder;

    @Getter private static final File rootSearchPath = new File(System.getProperty("user.home"));

    public static final String FILE = "file";
    public static final String FOLDER = "folder";

    @Getter private static List<Integer> uuidList;

    static
    {
        projectIDNameDict = new HashMap<Integer, String>();
        projectNameIDDict = new HashMap<String, String>();

        uuidList = new ArrayList<>();

        projectIDGenerator =  new AtomicInteger(0);
        uuidGenerator =  new AtomicInteger(0);

    }


    public static boolean isDatabaseUpdating()
    {
        return isDatabaseUpdating;
    }

    public static boolean isWindowOpen()
    {
        return isWindowOpen;
    }

    public static void setProjectNameNID(@NonNull String projectName,@NonNull Integer projectID)
    {
        projectNameIDDict.put(projectName, projectID);
        projectIDNameDict.put(projectID, projectName);
    }

    public static void setUUIDGenerator(Integer integer)
    {
        uuidGenerator = new AtomicInteger(integer);
    }

    public static boolean initSelector(String selection)
    {
        if((selection.equals(FILE)) || selection.equals(FOLDER))
        {
            currentWindowSelection = selection;
        }
        else
        {
            log.error("Wrong file type for opening. Set param either as file or folder");
            return false;
        }
        return true;
    }

    private static void initiateDatabaseUpdate()
    {
        isDatabaseUpdating = true;

        //JsonArray uuidListArray = response.getJsonArray(ServerConfig.UUID_LIST_PARAM);

        //SelectorHandler.configureUUIDGenerator(ConversionHandler.jsonArray2IntegerList(uuidListArray));
    }

    public static boolean isProjectNameRegistered(String projectName)
    {
        return projectNameIDDict.containsKey(projectName);
    }

    public static void setProjectNameBuffer(String projectName)
    {
        projectNameBuffer = projectName;
    }

    public static void processSelectorOutput(List<File> files)
    {
        setWindowState(false);

        if((files != null) && (!files.isEmpty()) && (files.get(0) != null))
        {
            initiateDatabaseUpdate();

            if(currentWindowSelection.equals(FILE))
            {
                fileHolder = files;

                generateUUID(fileHolder);
            }
            else if(currentWindowSelection.equals(FOLDER))
            {
                generateUUIDwithIteration(files.get(0));
            }

            if((fileHolder.isEmpty() == false) && (uuidList.size() == fileHolder.size()))
            {
                //update portfolio table
                PortfolioVerticle.updateUUIDList(uuidList);

                //update project table
                ProjectVerticle.updateUUIDList(fileHolder, uuidList);
            }

            //it's important to set database updating as false here as front end will start retrieving these
            isDatabaseUpdating = false;
        }

        clearProjectNameBuffer();
    }

    private static void clearProjectNameBuffer()
    {
        projectNameBuffer = "";
    }

    /**
     * @param state true = open, false = close
     */
    public static void setWindowState(boolean state)
    {
        isWindowOpen = state;
    }

    public static void configureUUIDGenerator(List<Integer> uuidList)
    {
        if(uuidList.isEmpty())
        {
            setUUIDGenerator(0);
        }
        else
        {
            Integer currentMaxUUID = Collections.max(uuidList);

            setUUIDGenerator(currentMaxUUID);
        }
    }

    public static void generateUUID(List<File> filesList)
    {
        uuidList = new ArrayList<>();

        for(File item : filesList)
        {
            uuidList.add(generateUUID());
        }
    }

    public static void generateUUIDwithIteration(@NonNull File rootDataPath)
    {
        fileHolder = new ArrayList<>();
        uuidList = new ArrayList<>();
        Stack<File> folderStack = new Stack<>();

        folderStack.push(rootDataPath);

        List<String> acceptableFileFormats = ImageFileType.getAllowedFileTypes();

        while(folderStack.isEmpty() != true)
        {
            File currentFolderPath = folderStack.pop();

            File[] folderList = currentFolderPath.listFiles();

            try
            {
                for(File file : folderList)
                {
                    if (file.isDirectory())
                    {
                        folderStack.push(file);
                    }
                    else
                    {
                        String absPath = file.getAbsolutePath();

                        for (String allowedFormat : acceptableFileFormats)
                        {
                            if(absPath.length() > allowedFormat.length())
                            {
                                String currentFormat = absPath.substring(absPath.length()  - allowedFormat.length());

                                if(currentFormat.equals(allowedFormat))
                                {
                                    fileHolder.add(file);
                                    uuidList.add(generateUUID());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            catch(Exception e)
            {
                log.info("Error occured while iterating data paths: ", e);
            }

        }
    }

    public static Integer generateProjectID()
    {
        return projectIDGenerator.incrementAndGet();
    }

    private static Integer generateUUID()
    {
        return uuidGenerator.incrementAndGet();
    }



    public static void setProjectIDGenerator(Integer seedNumber)
    {
        projectIDGenerator = new AtomicInteger(seedNumber);
    }

}