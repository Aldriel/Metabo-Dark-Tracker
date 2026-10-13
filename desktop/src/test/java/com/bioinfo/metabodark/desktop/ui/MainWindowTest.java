package com.bioinfo.metabodark.desktop.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.desktop.AppInfo;
import com.bioinfo.metabodark.desktop.FxThread;
import com.bioinfo.metabodark.desktop.i18n.Messages;
import com.bioinfo.metabodark.desktop.model.DesktopModel;
import com.bioinfo.metabodark.desktop.prefs.UserPreferences;
import com.bioinfo.metabodark.desktop.view.DocumentSaver;
import com.bioinfo.metabodark.desktop.view.FilePicker;
import com.bioinfo.metabodark.desktop.view.PickedFile;
import com.bioinfo.metabodark.example.SimulatedStudy;
import com.bioinfo.metabodark.model.Language;
import com.bioinfo.metabodark.service.MetaboService;

import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

/** The window, driven like a user on headless JavaFX. */
class MainWindowTest {

    @BeforeAll
    static void javaFx() {
        FxThread.start();
    }

    @Test
    void aVisitorAnnotatesTheExampleAndSavesTheReport() {
        AtomicReference<PickedFile> picked = new AtomicReference<>();
        List<String> urls = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<byte[]> bytes = new ArrayList<>();
        boolean[] quit = {false};
        Preferences node = Preferences.userRoot().node("metabodark-ui-" + UUID.randomUUID());
        DesktopModel model = new DesktopModel(new MetaboService(), new UserPreferences(node));
        FilePicker files = () -> picked.get();
        DocumentSaver saver =
                new DocumentSaver() {
                    @Override
                    public void saveText(String suggestedName, String content) {
                        texts.add(content);
                    }

                    @Override
                    public void saveBytes(String suggestedName, byte[] content) {
                        bytes.add(content);
                    }
                };
        FxThread.run(
                () -> {
                    MainController controller =
                            new MainController(
                                    model, files, saver, urls::add, () -> quit[0] = true);
                    Stage stage = new Stage();
                    stage.setScene(new Scene(controller.root(), 1100, 760));
                    stage.show();
                    controller.view.reportButton.fire();
                    controller.view.exportItem.fire();
                    controller.view.openItem.fire();
                    picked.set(new PickedFile("nope.mzML", "nope".getBytes()));
                    controller.view.openItem.fire();
                    picked.set(null);
                    controller.view.exampleItem.fire();
                    TableView<?> table = (TableView<?>) stage.getScene().lookup("#results");
                    assertTrue(table.getItems().size() > 0);
                    for (int row = 0; row < table.getItems().size(); row++) {
                        controller.view.scanColumn.getCellData(row);
                        controller.view.rtColumn.getCellData(row);
                        controller.view.mzColumn.getCellData(row);
                        controller.view.nameColumn.getCellData(row);
                        controller.view.familyColumn.getCellData(row);
                        controller.view.kindColumn.getCellData(row);
                        controller.view.cosineColumn.getCellData(row);
                    }
                    controller.view.englishItem.fire();
                    String author =
                            ((javafx.scene.control.Labeled) stage.getScene().lookup("#author"))
                                    .getText();
                    assertTrue(author.contains("Biocomputing"));
                    controller.view.largerItem.fire();
                    assertTrue(controller.root().getStyle().contains("14px"));
                    controller.view.kofiButton.fire();
                    controller.view.websiteButton.fire();
                    controller.view.guideItem.fire();
                    assertTrue(stage.getScene().lookup("#helpText").isVisible());
                    controller.view.frenchItem.fire();
                    controller.view.websiteButton.fire();
                    controller.view.guideItem.fire();
                    controller.view.exportItem.fire();
                    controller.view.reportTitle.setText("Window");
                    controller.view.reportButton.fire();
                    controller.view.tolerance.setText("nope");
                    controller.view.tolerance.fireEvent(new javafx.event.ActionEvent());
                    controller.view.tolerance.setText("5");
                    controller.view.applyButton.fire();
                    controller.view.englishItem.fire();
                    controller.view.cosine.setText("-1");
                    controller.view.cosine.fireEvent(new javafx.event.ActionEvent());
                    controller.view.tolerance.setText("0.02");
                    controller.view.cosine.setText("0.70");
                    controller.view.queryPublic.setSelected(true);
                    controller.view.applyButton.fire();
                    controller.view.websiteButton.fire();
                    shortcut(controller.root(), KeyCode.A, false);
                    shortcut(controller.root(), KeyCode.A, true);
                    shortcut(controller.root(), KeyCode.MINUS, true);
                    shortcut(controller.root(), KeyCode.EQUALS, true);
                    shortcut(controller.root(), KeyCode.PLUS, true);
                    shortcut(controller.root(), KeyCode.DIGIT0, true);
                    controller.view.smallerItem.fire();
                    controller.view.resetFontItem.fire();
                    picked.set(new PickedFile("sample.mzML", SimulatedStudy.bytes()));
                    controller.view.openItem.fire();
                    controller.view.quitItem.fire();
                    assertEquals("", MainController.explain(Messages.of(Language.EN), null));
                    assertEquals("", MainController.explain(Messages.of(Language.EN), " "));
                    assertEquals(
                            "The file could not be read.",
                            MainController.explain(Messages.of(Language.EN), "NOT_A_CODE"));
                    assertEquals("", MainController.number(Double.NaN));
                    assertEquals("1.5000", MainController.number(1.5));
                });
        assertTrue(quit[0]);
        assertEquals(AppInfo.KOFI, urls.get(0));
        assertTrue(urls.contains(AppInfo.WEBSITE_EN));
        assertTrue(urls.contains(AppInfo.WEBSITE_FR));
        assertTrue(texts.get(0).contains("scan"));
        assertTrue(bytes.get(0).length > 4);
    }

    private static void shortcut(javafx.scene.Parent root, KeyCode code, boolean control) {
        root.fireEvent(
                new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false));
    }
}
