package ai_server_cafe.gui.component;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.interfaces.IComponentCafe;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.EnumDisplayType;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeTableCafe extends JTable implements IComponentCafe {
    private final String name;
    private final EnumVisibleType type;
    private EnumDisplayType xType;
    private EnumDisplayType yType;
    private EnumDisplayType widthType;
    private EnumDisplayType heightType;
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public TreeTableCafe(String name, TreeTableNode root, double x, double y, double width, double height, EnumVisibleType type) {
        super();
        this.name = name;
        this.type = type;
        this.xType = EnumDisplayType.DEFAULT;
        this.yType = EnumDisplayType.DEFAULT;
        this.widthType = EnumDisplayType.DEFAULT;
        this.heightType = EnumDisplayType.DEFAULT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // フォーカスが外れたら自動で編集を確定
        this.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        setFont(new Font(config.font, config.fontStyle, config.fontSize));
        this.setLayout(null);

        TreeTableModel treeTableModel = new TreeTableModel(root);
        TreeTableRenderer renderer = new TreeTableRenderer(this, treeTableModel);

        this.setModel(new TreeTableModelAdapter(treeTableModel, renderer));
        this.setDefaultRenderer(TreeTableModel.class, renderer);
        this.setDefaultRenderer(String.class, this.getDefaultRenderer(Object.class));
        // １つの列で複数の型を使えるように
        this.setDefaultRenderer(Object.class, new Renderer());

        TreeTableSelectionModel selectionModel = new TreeTableSelectionModel();
        this.setSelectionModel(selectionModel.getListSelectionModel());

        TreeTableCellEditor cellEditor = new TreeTableCellEditor(renderer, this);
        this.setDefaultEditor(TreeTableModel.class, cellEditor);
        this.setDefaultEditor(Object.class, new Editor(this));
    }

    public void setColumnData(@Nonnull List<Pair<Class<?>, String>> columnData) {
        TreeTableModelAdapter model = (TreeTableModelAdapter) this.getModel();
        columnData.addFirst(new Pair<>(TreeTableModel.class, this.name));
        model.setColumnData(columnData);
    }

    public TreeTableCafe setDisplayType(int id, EnumDisplayType type) {
        if (id == 1)
            this.yType = type;
        else if (id == 2)
            this.widthType = type;
        else if (id == 3)
            this.heightType = type;
        else
            this.xType = type;
        return this;
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }

    @Override
    public Rectangle setPaneSize(int x, int y) {
        Rectangle rect = EnumDisplayType.getSize(new Vector2D(this.x, this.y), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(x, y));
        this.setBounds(rect);
        return rect;
    }

    public static class TreeTableNode {
        protected List<Pair<Object, IFuncParam1<Void, Object>>> data = new ArrayList<>();
        protected String name;
        private List<TreeTableNode> children = new ArrayList<>();

        public TreeTableNode(String name) {
            this.name = name;
        }

        public TreeTableNode(String name, @NonNull List<TreeTableNode> children) {
            this(name);
            this.children = children;
        }

        public void addData(Object data, IFuncParam1<Void, Object> setDataFunc) {
            this.data.add(new Pair<>(data, setDataFunc));
        }

        public void addChild(TreeTableNode child) {
            this.children.add(child);
        }

        public List<Pair<Object, IFuncParam1<Void, Object>>> getData() {
            return this.data;
        }

        public List<TreeTableNode> getChildren() {
            return this.children;
        }

        // treeで表示する名前
        public String toString() {
            return name;
        }
    }

    class TreeTableModel implements TreeModel {
        TreeTableNode root;

        public TreeTableModel(TreeTableNode root) {
            this.root = root;
        }

        @Override
        public Object getRoot() {
            return this.root;
        }

        @Override
        public Object getChild(Object parent, int index) {
            return ((TreeTableNode) parent).getChildren().get(index);
        }

        @Override
        public int getChildCount(Object parent) {
            return ((TreeTableNode) parent).getChildren().size();
        }

        @Override
        public boolean isLeaf(Object node) {
            return this.getChildCount(node) == 0;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {

        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            return ((TreeTableNode) parent).children.indexOf((TreeTableNode) child);
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {

        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {

        }
    }

    // tree表示用
    class TreeTableRenderer extends JTree implements TableCellRenderer {
        private int visibleRow;
        private final TreeTableCafe treeTable;

        public TreeTableRenderer(TreeTableCafe treeTable, TreeModel model) {
            super(model);
            this.treeTable = treeTable;
            setRowHeight(getRowHeight());
        }

        @Override
        public void setRowHeight(int rowHeight) {
            if (rowHeight > 0) {
                super.setRowHeight(rowHeight);
                if (treeTable != null && treeTable.getRowHeight() != rowHeight) {
                    treeTable.setRowHeight(getRowHeight());
                }
            }
        }

        @Override
        public void paint(Graphics g) {
            g.translate(0, -visibleRow * getRowHeight());
            super.paint(g);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected)
                setBackground(table.getSelectionBackground());
            else
                setBackground(table.getBackground());
            this.visibleRow = row;
            return this;
        }
    }

    class TreeTableModelAdapter extends AbstractTableModel {
        JTree tree;
        TreeTableModel treeTableModel;
        List<Pair<Class<?>, String>> columnData = new ArrayList<>();


        public TreeTableModelAdapter(TreeTableModel treeTableModel, JTree tree) {
            this.tree = tree;
            this.treeTableModel = treeTableModel;

            this.tree.addTreeExpansionListener(new TreeExpansionListener() {
                public void treeExpanded(TreeExpansionEvent event) {
                    int firstRow = tree.getRowForPath(event.getPath());
                    int lastRow = firstRow + tree.getModel().getChildCount(event.getPath().getLastPathComponent());
                    fireTableRowsInserted(firstRow, lastRow);
                }

                public void treeCollapsed(TreeExpansionEvent event) {
                    int firstRow = tree.getRowForPath(event.getPath());
                    int lastRow = firstRow + tree.getModel().getChildCount(event.getPath().getLastPathComponent());
                    fireTableRowsDeleted(firstRow, lastRow);
                }
            });
        }

        public void setColumnData(@Nonnull List<Pair<Class<?>, String>> columnData) {
            this.columnData = columnData;
            fireTableStructureChanged();
        }

        @Override
        public int getRowCount() {
            return this.tree.getRowCount();
        }

        @Override
        public int getColumnCount() {
            return this.columnData.size();
        }

        @Nls
        @Override
        public String getColumnName(int columnIndex) {
            String name;
            if (columnData.size() > columnIndex)
                name = columnData.get(columnIndex).getSecond();
            else
                name = "";
            return name;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            Class<?> clazz;
            if (columnData.size() > columnIndex)
                clazz = columnData.get(columnIndex).getFirst();
            else
                clazz = Object.class;
            return clazz;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            // 一列目はツリーにするため不要
            if (columnIndex == 0) {
                return null;
            }
            Object component = tree.getPathForRow(rowIndex).getLastPathComponent();
            if (component instanceof TreeTableNode node) {
                if (node.getData().size() > columnIndex - 1) {
                    return node.getData().get(columnIndex - 1).getFirst();
                }
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            // ツリーは無視
            if (columnIndex == 0) {
                return;
            }
            Object component = tree.getPathForRow(rowIndex).getLastPathComponent();
            if (component instanceof TreeTableNode node) {
                if (node.getData().size() > columnIndex - 1) {
                    Pair<Object, IFuncParam1<Void, Object>> data = node.getData().get(columnIndex - 1);
                    IFuncParam1<Void, Object> func = node.getData().get(columnIndex - 1).getSecond();
                    if (func != null)
                        func.function(aValue);

                    // 表示も変える
                    node.getData().set(columnIndex - 1, new Pair<>(aValue, data.getSecond()));
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    class TreeTableSelectionModel extends DefaultTreeSelectionModel {
        public TreeTableSelectionModel() {
            super();
            getListSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {

                }
            });
        }

        ListSelectionModel getListSelectionModel() {
            return listSelectionModel;
        }
    }

    class TreeTableCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTree tree;
        private final JTable table;

        public TreeTableCellEditor(JTree tree, JTable table) {
            this.tree = tree;
            this.table = table;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return tree;
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent me) {
                int column1 = 0;
                int doubleClick = 2;
                MouseEvent newME = new MouseEvent(tree, me.getID(), me.getWhen(), me.getModifiers(), me.getX() - table.getCellRect(0, column1, true).x, me.getY(), doubleClick, me.isPopupTrigger());
                tree.dispatchEvent(newME);
            }
            return false;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }

    class Renderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return null;
            }
            Class<?> clazz = value.getClass();
            // 標準で表示できるクラスならそれを使う
            TableCellRenderer defaultRenderer = table.getDefaultRenderer(clazz);
            if (defaultRenderer.getClass() != this.getClass()) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (clazz.isArray()) {
                if (!clazz.getComponentType().isPrimitive()) {
                    defaultRenderer = table.getDefaultRenderer(String.class);
                    return defaultRenderer.getTableCellRendererComponent(table, Arrays.toString((Object[]) value), isSelected, hasFocus, row, column);
                } else if (clazz == int[].class) {
                    defaultRenderer = table.getDefaultRenderer(String.class);
                    return defaultRenderer.getTableCellRendererComponent(table, Arrays.toString((int[]) value), isSelected, hasFocus, row, column);
                }
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    class Editor extends AbstractCellEditor implements TableCellEditor {
        private final Map<Class<?>, TableCellEditor> cellEditors = new HashMap<>();
        // 使用するeditor
        private TableCellEditor useEditor;

        public Editor(JTable table) {

            // クラス毎のeditorの設定
            this.cellEditors.put(String.class, new DefaultCellEditor(new TextFieldCafe("", 0, 0, 10, null, EnumVisibleType.ALWAYS)));
            this.cellEditors.put(Boolean.class, table.getDefaultEditor(Boolean.class));
            JTextField numberTextField = new TextFieldCafe("", 0, 0, 0, null, EnumVisibleType.ALWAYS);
            numberTextField.setHorizontalAlignment(JTextField.RIGHT);
            this.cellEditors.put(Number.class, new DefaultCellEditor(numberTextField) {
                Class<?> clazz;

                @Override
                public Object getCellEditorValue() {
                    Number number = null;
                    String value = (String) super.getCellEditorValue();
                    if (clazz == Integer.class) {
                        number = Integer.parseInt(value);
                    } else if (clazz == Double.class) {
                        number = Double.parseDouble(value);
                    } else if (clazz == Long.class) {
                        number = Long.parseLong(value);
                    }
                    return number;
                }

                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    this.clazz = value.getClass();
                    return super.getTableCellEditorComponent(table, value, isSelected, row, column);
                }
            });

            this.cellEditors.put(Enum.class, new DefaultCellEditor(new JComboBox<>()) {
                JComboBox comboBox;

                @Override
                public Object getCellEditorValue() {
                    return comboBox.getSelectedItem();
                }

                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    comboBox = new ComboBoxCafe<>(value.getClass().getEnumConstants(), value, 0, 0, 0, 0, null, EnumVisibleType.ALWAYS);
                    comboBox.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // 項目を選択したら編集をやめる
                            stopCellEditing();
                        }
                    });
                    return comboBox;
                }
            });

            this.cellEditors.put(Object[].class, new DefaultCellEditor(new JTextField()) {
                Object value;

                @Override
                public Object getCellEditorValue() {
                    String value = (String) delegate.getCellEditorValue();
                    List<String> stringList = Arrays.stream(value.substring(1, value.length() - 1).split(", ")).toList();
                    Object[] objects = stringList.toArray();
                    if (!stringList.isEmpty())
                        if (this.value.getClass().getComponentType() == String.class) {
                            String[] strings = Arrays.copyOf(objects, stringList.size(), String[].class);
                            return strings;
                        }
                    return null;
                }

                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    this.value = value;
                    return super.getTableCellEditorComponent(table, Arrays.toString((Object[]) value), isSelected, row, column);
                }
            });

            this.cellEditors.put(int[].class, new DefaultCellEditor(new TextFieldCafe("", 0, 0, 0, null, EnumVisibleType.ALWAYS)) {
                @Override
                public Object getCellEditorValue() {
                    String value = (String) delegate.getCellEditorValue();
                    List<String> stringList = Arrays.stream(value.substring(1, value.length() - 1).split(", ")).toList();
                    if (!stringList.isEmpty())
                        return stringList.stream().mapToInt(Integer::parseInt).toArray();
                    return new int[0];
                }

                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    return super.getTableCellEditorComponent(table, Arrays.toString((int[]) value), isSelected, row, column);
                }
            });
        }

        @Override
        public Object getCellEditorValue() {
            return useEditor.getCellEditorValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value != null) {
                Class<?> clazz = value.getClass();
                this.useEditor = getCellEditor(clazz);
                if (this.useEditor != null) {
                    return this.useEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                } else if (clazz.isArray()) {
                    this.useEditor = getCellEditor(Object[].class);
                    return this.useEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
                }
            }
            return null;
        }

        public TableCellEditor getCellEditor(Class<?> clazz) {
            if (clazz == null) {
                return null;
            } else {
                TableCellEditor editor = cellEditors.get(clazz);
                if (editor != null) {
                    return editor;
                } else {
                    // 親クラスを見る
                    return getCellEditor(clazz.getSuperclass());
                }
            }
        }

        public boolean stopCellEditing() {
            useEditor.stopCellEditing();
            return super.stopCellEditing();
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            listenerList.add(CellEditorListener.class, l);
            for (Map.Entry<Class<?>, TableCellEditor> entry : cellEditors.entrySet()) {
                entry.getValue().addCellEditorListener(l);
            }
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(CellEditorListener.class, l);
            for (Map.Entry<Class<?>, TableCellEditor> entry : cellEditors.entrySet()) {
                entry.getValue().removeCellEditorListener(l);
            }
        }
    }
}