/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bioinfo.metabodark.desktop;

import com.bioinfo.metabodark.catalog.HttpCatalog;
import com.bioinfo.metabodark.desktop.model.DesktopModel;
import com.bioinfo.metabodark.desktop.prefs.UserPreferences;
import com.bioinfo.metabodark.desktop.ui.MainController;
import com.bioinfo.metabodark.desktop.view.NativeDocumentSaver;
import com.bioinfo.metabodark.desktop.view.NativeFilePicker;
import com.bioinfo.metabodark.library.ReferenceLibrary;
import com.bioinfo.metabodark.service.MetaboService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** JavaFX application. The window is assembled by {@link MainController}. */
public class DesktopApp extends Application {

    @Override
    public void start(Stage stage) {
        MetaboService service =
                new MetaboService(ReferenceLibrary.entries(), HttpCatalog.defaults());
        DesktopModel model = new DesktopModel(service, new UserPreferences());
        MainController controller =
                new MainController(
                        model,
                        new NativeFilePicker(stage),
                        new NativeDocumentSaver(stage),
                        url -> getHostServices().showDocument(url),
                        Platform::exit);
        Scene scene = new Scene(controller.root(), 1100, 760);
        scene.getStylesheets()
                .add(DesktopApp.class.getResource("view/styles.css").toExternalForm());
        stage.setTitle(AppInfo.NAME);
        stage.setMinWidth(880);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }
}
