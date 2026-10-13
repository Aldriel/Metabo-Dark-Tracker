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

import com.bioinfo.metabodark.desktop.AppInfo;
import com.bioinfo.metabodark.desktop.i18n.Messages;
import com.bioinfo.metabodark.desktop.model.DesktopModel;
import com.bioinfo.metabodark.desktop.view.DocumentSaver;
import com.bioinfo.metabodark.desktop.view.FilePicker;
import com.bioinfo.metabodark.desktop.view.PickedFile;
import com.bioinfo.metabodark.model.Language;
import com.bioinfo.metabodark.service.Analysis;

import javafx.beans.binding.Bindings;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Connects {@link MainView} to {@link DesktopModel}. The view is filled again after every action.
 */
public final class MainController {

    private final DesktopModel model;
    private final FilePicker files;
    private final DocumentSaver saver;
    private final Consumer<String> openUrl;
    private final Runnable quit;
    final MainView view = new MainView();
    private boolean filling;

    /**
     * Builds the controller.
     *
     * @param model application state
     * @param files how a spectrum file is chosen
     * @param saver how a report is saved
     * @param openUrl opens a web address
     * @param quit closes the application
     */
    public MainController(
            DesktopModel model,
            FilePicker files,
            DocumentSaver saver,
            Consumer<String> openUrl,
            Runnable quit) {
        this.model = model;
        this.files = files;
        this.saver = saver;
        this.openUrl = openUrl;
        this.quit = quit;
        wire();
        refresh();
    }

    /** Root node, ready to put in a scene. */
    public Parent root() {
        return view.root();
    }

    private void wire() {
        view.openItem.setOnAction(event -> openFile());
        view.exampleItem.setOnAction(
                event -> {
                    model.openExample();
                    refresh();
                });
        view.exportItem.setOnAction(event -> export());
        view.quitItem.setOnAction(event -> quit.run());
        view.frenchItem.setOnAction(
                event -> {
                    if (!filling) {
                        language(Language.FR);
                    }
                });
        view.englishItem.setOnAction(
                event -> {
                    if (!filling) {
                        language(Language.EN);
                    }
                });
        view.smallerItem.setOnAction(event -> font(-1));
        view.largerItem.setOnAction(event -> font(1));
        view.resetFontItem.setOnAction(
                event -> {
                    model.resetFont();
                    refresh();
                });
        view.guideItem.setOnAction(event -> toggleHelp());
        view.kofiButton.setOnAction(event -> openUrl.accept(AppInfo.KOFI));
        view.websiteButton.setOnAction(event -> openUrl.accept(website()));
        view.applyButton.setOnAction(event -> apply());
        view.tolerance.setOnAction(event -> apply());
        view.cosine.setOnAction(event -> apply());
        view.reportButton.setOnAction(event -> buildReport());
        view.root().addEventFilter(KeyEvent.KEY_PRESSED, this::shortcut);
        view.scanColumn.setCellValueFactory(
                cell ->
                        Bindings.createStringBinding(
                                () -> Integer.toString(cell.getValue().scan())));
        view.rtColumn.setCellValueFactory(
                cell ->
                        Bindings.createStringBinding(
                                () -> number(cell.getValue().retentionSeconds())));
        view.mzColumn.setCellValueFactory(
                cell -> Bindings.createStringBinding(() -> number(cell.getValue().precursorMz())));
        view.nameColumn.setCellValueFactory(
                cell -> Bindings.createStringBinding(() -> cell.getValue().name()));
        view.familyColumn.setCellValueFactory(
                cell ->
                        Bindings.createStringBinding(
                                () ->
                                        label(
                                                messages(),
                                                "family." + cell.getValue().family().name())));
        view.kindColumn.setCellValueFactory(
                cell ->
                        Bindings.createStringBinding(
                                () -> label(messages(), "kind." + cell.getValue().kind().name())));
        view.cosineColumn.setCellValueFactory(
                cell -> Bindings.createStringBinding(() -> number(cell.getValue().cosine())));
    }

    private void shortcut(KeyEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }
        if (event.getCode() == KeyCode.MINUS) {
            font(-1);
        } else if (event.getCode() == KeyCode.EQUALS || event.getCode() == KeyCode.PLUS) {
            font(1);
        } else if (event.getCode() == KeyCode.DIGIT0) {
            model.resetFont();
            refresh();
        } else {
            return;
        }
        event.consume();
    }

    private void openFile() {
        PickedFile file = files.open();
        if (file == null) {
            return;
        }
        model.open(file.content(), file.name(), false);
        refresh();
    }

    private void language(Language language) {
        model.language(language);
        refresh();
    }

    private void font(int delta) {
        model.changeFont(delta);
        refresh();
    }

    private void toggleHelp() {
        boolean show = !view.help.isVisible();
        view.help.setVisible(show);
        view.help.setManaged(show);
        view.help.setText(messages().guide());
    }

    private void apply() {
        try {
            double tolerance = Double.parseDouble(view.tolerance.getText().trim());
            double cosine = Double.parseDouble(view.cosine.getText().trim());
            String code = model.setNumbers(tolerance, cosine, view.queryPublic.isSelected());
            if (code != null) {
                view.status.setText(explain(messages(), code));
                return;
            }
        } catch (NumberFormatException e) {
            view.status.setText(explain(messages(), "NUMBER"));
            return;
        }
        refresh();
    }

    private void export() {
        if (model.analysis() == null) {
            view.status.setText(explain(messages(), "NO_FILE"));
            return;
        }
        saver.saveText("Metabo-Dark.tsv", model.export());
        view.status.setText(messages().get("status.saved"));
    }

    private void buildReport() {
        if (model.analysis() == null) {
            view.status.setText(explain(messages(), "NO_FILE"));
            return;
        }
        saver.saveBytes("Metabo-Dark.pdf", model.report(view.reportTitle.getText()));
        view.status.setText(messages().get("status.saved"));
    }

    private void refresh() {
        filling = true;
        Messages text = messages();
        view.root().setStyle("-fx-font-size: " + model.fontSize() + "px;");
        view.fileMenu.setText(text.get("menu.file"));
        view.openItem.setText(text.get("menu.open"));
        view.exampleItem.setText(text.get("menu.example"));
        view.exportItem.setText(text.get("menu.export"));
        view.quitItem.setText(text.get("menu.quit"));
        view.optionsMenu.setText(text.get("menu.options"));
        view.languageMenu.setText(text.get("menu.language"));
        view.frenchItem.setText(text.get("menu.french"));
        view.englishItem.setText(text.get("menu.english"));
        view.fontMenu.setText(text.get("menu.font"));
        view.smallerItem.setText(text.get("menu.smaller"));
        view.largerItem.setText(text.get("menu.larger"));
        view.resetFontItem.setText(text.get("menu.reset"));
        view.helpMenu.setText(text.get("menu.help"));
        view.guideItem.setText(text.get("menu.guide"));
        view.frenchItem.setSelected(model.language() == Language.FR);
        view.englishItem.setSelected(model.language() == Language.EN);
        view.intro.setText(text.get("status.ready"));
        view.tolerance.setPromptText(text.get("field.tolerance"));
        view.cosine.setPromptText(text.get("field.cosine"));
        view.queryPublic.setText(text.get("field.public"));
        view.applyButton.setText(text.get("field.apply"));
        view.reportTitle.setPromptText(text.get("field.title"));
        view.reportButton.setText(text.get("field.report"));
        view.scanColumn.setText(text.get("column.scan"));
        view.rtColumn.setText(text.get("column.rt"));
        view.mzColumn.setText(text.get("column.mz"));
        view.nameColumn.setText(text.get("column.name"));
        view.familyColumn.setText(text.get("column.family"));
        view.kindColumn.setText(text.get("column.kind"));
        view.cosineColumn.setText(text.get("column.cosine"));
        view.author.setText(text.get("footer.author"));
        view.email.setText(AppInfo.CONTACT_EMAIL);
        view.availability.setText(text.get("footer.availability"));
        view.kofiButton.setText(text.get("footer.kofi"));
        view.websiteButton.setText(text.get("footer.website"));
        view.tolerance.setText(Double.toString(model.settings().mzToleranceDa()));
        view.cosine.setText(Double.toString(model.settings().cosineMinimum()));
        view.queryPublic.setSelected(model.settings().queryPublic());
        Analysis analysis = model.analysis();
        if (analysis == null) {
            view.table.getItems().clear();
            view.status.setText(explain(text, model.errorCode()));
        } else {
            view.table.getItems().setAll(analysis.annotations());
            String summary =
                    MessageFormat.format(
                            text.get("status.summary"),
                            analysis.ms2(),
                            analysis.libraryHits(),
                            analysis.familyHits(),
                            analysis.dark());
            if (model.simulated()) {
                summary = summary + " " + text.get("status.simulated");
            }
            if (model.errorCode() != null) {
                view.status.setText(explain(text, model.errorCode()));
            } else {
                view.status.setText(model.fileName() + " — " + summary);
            }
        }
        if (view.help.isVisible()) {
            view.help.setText(text.guide());
        }
        filling = false;
    }

    private String website() {
        return model.language() == Language.FR ? AppInfo.WEBSITE_FR : AppInfo.WEBSITE_EN;
    }

    private Messages messages() {
        return Messages.of(model.language());
    }

    /**
     * Translates an error code. A missing translation becomes the generic message.
     *
     * @param text current language
     * @param code stable code, or {@code null}
     * @return the sentence shown in the status line
     */
    static String explain(Messages text, String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return label(text, "error." + code);
    }

    /**
     * Looks up a key and falls back to the generic error when the key is absent.
     *
     * @param text current language
     * @param key message key
     * @return the text
     */
    static String label(Messages text, String key) {
        String value = text.get(key);
        if (value.startsWith("!")) {
            return text.get("error.unknown");
        }
        return value;
    }

    /**
     * Formats a finite number with four decimals. Anything else is blank.
     *
     * @param value the number
     * @return the text
     */
    static String number(double value) {
        if (!Double.isFinite(value)) {
            return "";
        }
        return String.format(Locale.US, "%.4f", value);
    }
}
