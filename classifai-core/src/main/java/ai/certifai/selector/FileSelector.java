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

import ai.certifai.data.DataType;
import ai.certifai.data.type.image.ImageFileType;
import ai.certifai.util.ImageHandler;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Open browser to select files
 *
 * @author Chiawei Lim
 */
@Slf4j
public class FileSelector{
    private static FileNameExtensionFilter imgfilter = new FileNameExtensionFilter("Image Files", ImageFileType.getImageFileTypes());

    public void runFileSelector(String projectName, AtomicInteger uuidGenerator) {

        try {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Point pt = MouseInfo.getPointerInfo().getLocation();
                    JFrame jf = new JFrame();
                    jf.setAlwaysOnTop(true);
                    jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    jf.setLocation(pt);
                    jf.requestFocus();
                    jf.setVisible(false);

                    JFileChooser fc = new JFileChooser() {
                        @Override
                        protected JDialog createDialog(Component parent)
                                throws HeadlessException {
                            JDialog dialog = super.createDialog(parent);
                            dialog.setLocationByPlatform(true);
                            dialog.setAlwaysOnTop(true);
                            return dialog;
                        }
                    };

                    fc.setCurrentDirectory(SelectorHandler.getRootSearchPath());
                    fc.setFileFilter(imgfilter);
                    fc.setDialogTitle("Select Files");
                    fc.setMultiSelectionEnabled(true);
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int res = fc.showOpenDialog(jf);
                    jf.dispose();

                    if (res == JFileChooser.APPROVE_OPTION) {
                        java.util.List<File> files = new ArrayList<>(java.util.Arrays.asList(fc.getSelectedFiles()));

                        if((files != null) && (!files.isEmpty()) && (files.get(0) != null))
                        {
                            SelectorHandler.startDatabaseUpdate(projectName);

                            DataType dataType = SelectorHandler.getProjectDataType(projectName);

                            if (dataType == DataType.IMAGE) ImageHandler.processFile(files, uuidGenerator);

                            SelectorHandler.stopDatabaseUpdate();
                        }
                    }else
                    {
                        SelectorHandler.setWindowState(false);
                    }


                }
            });
        }
        catch (Exception e){
            SelectorHandler.setWindowState(false);

            log.debug("Select files failed to open", e);
        }

    }

}


