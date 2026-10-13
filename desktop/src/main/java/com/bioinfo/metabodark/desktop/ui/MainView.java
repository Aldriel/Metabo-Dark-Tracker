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

package com.bioinfo.metabodark.desktop.ui;

import com.bioinfo.metabodark.service.Annotation;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * JavaFX view: menus, the annotation table, the footer and the status line. It displays state and
 * exposes the controls; it does not annotate anything.
 */
public final class MainView {

    final MenuItem openItem = new MenuItem();
    final MenuItem exampleItem = new MenuItem();
    final MenuItem exportItem = new MenuItem();
    final MenuItem quitItem = new MenuItem();
    final RadioMenuItem frenchItem = new RadioMenuItem();
    final RadioMenuItem englishItem = new RadioMenuItem();
    final MenuItem smallerItem = new MenuItem();
    final MenuItem largerItem = new MenuItem();
    final MenuItem resetFontItem = new MenuItem();
    final MenuItem guideItem = new MenuItem();
    final Menu fileMenu = new Menu();
    final Menu optionsMenu = new Menu();
    final Menu languageMenu = new Menu();
    final Menu fontMenu = new Menu();
    final Menu helpMenu = new Menu();
    final Label intro = new Label();
    final TextField tolerance = field();
    final TextField cosine = field();
    final CheckBox queryPublic = new CheckBox();
    final Button applyButton = new Button();
    final TextField reportTitle = new TextField();
    final Button reportButton = new Button();
    final TableView<Annotation> table = new TableView<>();
    final TableColumn<Annotation, String> scanColumn = new TableColumn<>();
    final TableColumn<Annotation, String> rtColumn = new TableColumn<>();
    final TableColumn<Annotation, String> mzColumn = new TableColumn<>();
    final TableColumn<Annotation, String> nameColumn = new TableColumn<>();
    final TableColumn<Annotation, String> familyColumn = new TableColumn<>();
    final TableColumn<Annotation, String> kindColumn = new TableColumn<>();
    final TableColumn<Annotation, String> cosineColumn = new TableColumn<>();
    final TextArea help = new TextArea();
    final Label status = new Label();
    final Button kofiButton = new Button();
    final Button websiteButton = new Button();
    final Label availability = new Label();
    final Label author = new Label();
    final Label email = new Label();
    private final BorderPane root = new BorderPane();

    /** Builds the window contents. */
    public MainView() {
        ToggleGroup languages = new ToggleGroup();
        frenchItem.setToggleGroup(languages);
        englishItem.setToggleGroup(languages);
        fileMenu.getItems().addAll(openItem, exampleItem, exportItem, quitItem);
        languageMenu.getItems().addAll(frenchItem, englishItem);
        fontMenu.getItems().addAll(smallerItem, largerItem, resetFontItem);
        optionsMenu.getItems().addAll(languageMenu, fontMenu);
        helpMenu.getItems().add(guideItem);
        MenuBar bar = new MenuBar(fileMenu, optionsMenu, helpMenu);
        table.getColumns()
                .addAll(
                        scanColumn,
                        rtColumn,
                        mzColumn,
                        nameColumn,
                        familyColumn,
                        kindColumn,
                        cosineColumn);
        table.setId("results");
        table.setPrefHeight(280);
        help.setEditable(false);
        help.setWrapText(true);
        help.setVisible(false);
        help.setManaged(false);
        help.setId("helpText");
        status.setId("status");
        tolerance.setId("tolerance");
        cosine.setId("cosine");
        queryPublic.setId("queryPublic");
        applyButton.setId("applyButton");
        reportTitle.setId("reportTitle");
        reportButton.setId("reportButton");
        exampleItem.setId("exampleItem");
        kofiButton.setId("kofiButton");
        websiteButton.setId("websiteButton");
        author.setId("author");
        email.setId("email");
        availability.setId("availability");
        VBox center =
                new VBox(
                        8,
                        intro,
                        new HBox(8, tolerance, cosine, queryPublic, applyButton),
                        new HBox(8, reportTitle, reportButton),
                        table,
                        help);
        center.setPadding(new Insets(12));
        root.setTop(bar);
        root.setCenter(center);
        root.setBottom(footer());
        root.getStyleClass().add("metabodark-root");
        root.setId("root");
    }

    /** Root node of the window. */
    public Parent root() {
        return root;
    }

    private VBox footer() {
        HBox box = new HBox(12, author, email, websiteButton, availability, kofiButton);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.getStyleClass().add("footer");
        return new VBox(status, box);
    }

    private static TextField field() {
        TextField field = new TextField();
        field.setMaxWidth(160);
        return field;
    }
}
