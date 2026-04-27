package ai_server_cafe.gui.registry;

import ai_server_cafe.Main;
import ai_server_cafe.config.Config;
import ai_server_cafe.gui.ConfigArea;
import ai_server_cafe.gui.ConfigWindow;
import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.SplitPane;
import ai_server_cafe.gui.component.ButtonCafe;
import ai_server_cafe.gui.component.ScrollPaneCafe;
import ai_server_cafe.gui.component.TreeTableCafe;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.EnumDisplayType;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class RegistryConfigComponents {

    public static void registerContents(@Nonnull ConfigWindow window) {
        window.updateTitle();
        setComponentConfig(window);
    }

    private static void setComponentConfig(ConfigWindow window) {

        Config config = ConfigManager.getInstance().getEditableConfig();
        Field[] fields = config.getClass().getDeclaredFields();

        TreeTableCafe.TreeTableNode root = new TreeTableCafe.TreeTableNode("Config");

        MakeTree(root, fields, config);
        TreeTableCafe treeTableCafe = new TreeTableCafe("config", root, 0, 0, 1, 1, EnumVisibleType.ALWAYS)
                .setDisplayType(0, EnumDisplayType.RATIO)
                .setDisplayType(1, EnumDisplayType.RATIO)
                .setDisplayType(2, EnumDisplayType.RATIO)
                .setDisplayType(3, EnumDisplayType.RATIO);

        List<Pair<Class<?>, String>> columnData = new ArrayList<>();
        columnData.add(new Pair<>(Object.class, "value"));
        treeTableCafe.setColumnData(columnData);
        treeTableCafe.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    ConfigManager.getInstance().markDirty();
                    window.updateTitle();
                }
            }
        });

        ScrollPaneCafe<TreeTableCafe> sp = new ScrollPaneCafe<>(treeTableCafe);

        FileNameExtensionFilter JsonFilter = new FileNameExtensionFilter(
                "JSON File (*.json)", "json");

        // configの読み込み
        IFuncParam1<Void, Boolean> onSwitchLoad = new IFuncParam1<>() {
            @Override
            public Void function(Boolean aBoolean) {
                File dir = new File("config");
                JFileChooser fileChooser = new JFileChooser(dir);
                fileChooser.setFileFilter(JsonFilter);
                if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    if (ConfigManager.getInstance().isDirty()) {
                        String m = String.format("Save changes to %s?", ConfigManager.getInstance().getConfigSettings().configName);
                        int rv = JOptionPane.showConfirmDialog(
                                window, m, "Confirm Save", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (rv == JOptionPane.YES_OPTION) {
                            ConfigManager.getInstance().save();
                        } else if (rv == JOptionPane.CANCEL_OPTION || rv == JOptionPane.CLOSED_OPTION) {
                            return null;
                        }
                    }
                    ConfigManager.getInstance().loadFrom(fileChooser.getSelectedFile().getName());
                    window.reset();
                    window.setVisible(true);
                }
                return null;
            }
        };
        ButtonCafe loadButton = new ButtonCafe("Load", 0, 0, 1, 0.1, onSwitchLoad, EnumVisibleType.ALWAYS)
                .setDisplayType(0, EnumDisplayType.RATIO)
                .setDisplayType(1, EnumDisplayType.RATIO)
                .setDisplayType(2, EnumDisplayType.RATIO)
                .setDisplayType(3, EnumDisplayType.RATIO);

        // 名前をつけて保存
        IFuncParam1<Void, Boolean> onSwitchSaveAs = new IFuncParam1<>() {
            @Override
            public Void function(Boolean value) {
                File dir = new File("config");
                JFileChooser fileChooser = new JFileChooser(dir) {
                    @Override
                    public void approveSelection() {
                        String filePath = getSelectedFile().getPath();
                        if (!filePath.endsWith(".json"))
                            filePath += ".json";
                        File f = new File(filePath);
                        if (f.exists() && getDialogType() == SAVE_DIALOG) {
                            String m = String.format("%s is already exists. %nDo you want to replace it?", f.getName());
                            int rv = JOptionPane.showConfirmDialog(
                                    this, m, "Confirm Save As", JOptionPane.YES_NO_OPTION);
                            if (rv != JOptionPane.YES_OPTION) {
                                return;
                            }
                        }
                        super.approveSelection();
                    }
                };
                fileChooser.setFileFilter(JsonFilter);

                if (fileChooser.showSaveDialog(window) == JFileChooser.APPROVE_OPTION) {
                    ConfigManager.getInstance().saveAs(fileChooser.getSelectedFile().getName());
                }
                window.reset();
                window.setVisible(true);
                return null;
            }
        };
        ButtonCafe saveAsButton = new ButtonCafe("Save As", 0, 0.1, 1, 0.1, onSwitchSaveAs, EnumVisibleType.ALWAYS)
                .setDisplayType(0, EnumDisplayType.RATIO)
                .setDisplayType(1, EnumDisplayType.RATIO)
                .setDisplayType(2, EnumDisplayType.RATIO)
                .setDisplayType(3, EnumDisplayType.RATIO);

        // 上書き保存
        IFuncParam1<Void, Boolean> onSwitchSave = new IFuncParam1<>() {
            @Override
            public Void function(Boolean value) {
                ConfigManager.getInstance().save();
                window.updateTitle();
                return null;
            }
        };
        ButtonCafe saveButton = new ButtonCafe("Save", 0, 0.2, 1, 0.1, onSwitchSave, EnumVisibleType.ALWAYS)
                .setDisplayType(0, EnumDisplayType.RATIO)
                .setDisplayType(1, EnumDisplayType.RATIO)
                .setDisplayType(2, EnumDisplayType.RATIO)
                .setDisplayType(3, EnumDisplayType.RATIO);

        // configの適用（再起動）
        ButtonCafe applyButton = new ButtonCafe("Apply (reboot)", 0, 0.3, 1, 0.1,
                new IFuncParam1<>() {
                    @Override
                    @Nullable
                    public Void function(Boolean aBoolean) {
                        // 閉じるときに保存されるように
                        ConfigManager.getInstance().markDirty();
                        Main.exit(0, true);
                        window.reset();
                        window.setVisible(true);
                        return null;
                    }
                },
                EnumVisibleType.ALWAYS)
                .setDisplayType(0, EnumDisplayType.RATIO)
                .setDisplayType(1, EnumDisplayType.RATIO)
                .setDisplayType(2, EnumDisplayType.RATIO)
                .setDisplayType(3, EnumDisplayType.RATIO);


        // ショートカットキー
        Action shortcutLoadAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                loadButton.setSelected(true);
            }
        };
        loadButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(config.configEditorConfig.loadShortcut), "load");
        loadButton.getActionMap().put("load", shortcutLoadAction);

        Action shortcutSaveAsAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                saveAsButton.setSelected(true);
            }
        };
        saveAsButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(config.configEditorConfig.saveAsShortcut), "save");
        saveAsButton.getActionMap().put("save", shortcutSaveAsAction);

        Action shortcutSaveAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                saveButton.setSelected(true);
            }
        };
        saveButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(config.configEditorConfig.saveShortcut), "save as");
        saveButton.getActionMap().put("save as", shortcutSaveAction);

        Action shortcutApplyAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                applyButton.setSelected(true);
            }
        };
        applyButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(config.configEditorConfig.applyShortcut), "apply");
        applyButton.getActionMap().put("apply", shortcutApplyAction);

        // 表示位置の設定
        JPanel treeTablePanel = new ConfigArea(100, 0, window.getWidth() - 100, window.getHeight());
        treeTablePanel.add(sp);

        JPanel buttonPanel = new ConfigArea(0, 0, 100, window.getHeight());
        buttonPanel.add(loadButton);
        buttonPanel.add(saveAsButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(applyButton);

        window.setContentPane(new SplitPane(JSplitPane.HORIZONTAL_SPLIT, 100, buttonPanel, treeTablePanel));
        window.setResizable(true);
    }

    // configのデータをツリー構造にする
    private static void MakeTree(TreeTableCafe.TreeTableNode parent, Field[] fields, Object object) {
        try {
            for (Field field : fields) {
                TreeTableCafe.TreeTableNode node = new TreeTableCafe.TreeTableNode(field.getName());
                parent.addChild(node);

                Object data = field.get(object);
                IFuncParam1<Void, Object> setDataFunc = new IFuncParam1<>() {
                    @Override
                    public Void function(Object o) {
                        try {
                            field.set(object, o);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                };

                Field[] fields2 = field.getType().getFields();
                if (fields2.length > 0 && field.getType() != String.class && !fields2[0].isEnumConstant())
                    MakeTree(node, fields2, field.get(object));
                else {
                    node.addData(data, setDataFunc);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
