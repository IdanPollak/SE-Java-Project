package org.spa.ui.order;

import org.spa.common.util.log.Logger;
import org.spa.common.util.log.factory.LoggerFactory;
import org.spa.controller.item.WarehouseItem;
import org.spa.model.Item;
import org.spa.ui.cart.ShoppingCartView;
import org.spa.ui.control.CustomGridBagConstraints;
import org.spa.ui.item.ItemColumn;
import org.spa.ui.item.ItemViewInfo;
import org.spa.ui.table.TableConfig;
import org.spa.ui.table.TableManager;
import org.spa.ui.util.Controls;
import org.spa.ui.util.Fonts;
import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import static org.spa.ui.item.ItemCopying.warehouseItemToItemViewInfo;
import static org.spa.ui.util.Controls.*;
import static org.spa.ui.util.Controls.createTextArea;

public class OrderInfoDialog extends JFrame {
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private final OrderViewInfo order;
    private TableManager<ItemColumn, ItemViewInfo> tableManager;
    private List<ItemViewInfo> tableModelList;
    private OrderViewInfo orderItems;
    private static final Logger logger = LoggerFactory.getLogger(OrderInfoDialog.class);

    public OrderInfoDialog(OrderViewInfo order) {
        this.order = order;
        orderItems = new OrderViewInfo(order.getOrderId(),order.getOrderTime(),order.getUserId(),order.getItems());
    }

    public OrderInfoDialog init() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension((int) (screenSize.width / 1.5), (int) (screenSize.height / 2)));

        // These fucking Swing panels insist on not working as expected.. Hence I do this voodoo in order to repaint he window with
        // a different height after it has been displayed. Somehow it works and the "Price" row is displayed. Otherwise the price is
        // not displayed and the dialog must be resized by user
        SwingUtilities.invokeLater(() -> setPreferredSize(new Dimension((int) (screenSize.width / 1.5), (int) (screenSize.height / 1.8))));

        setTitle("Order Info");

        // Lay out the label and text panes from top to bottom.
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.PAGE_AXIS));
        fields.setAlignmentX(0);

        // Lay out the members from left to right.
        JPanel orderIdPanel = createMemberPanelWithLabel("Order ID:", order.getOrderId());
        JPanel datePanel = createMemberPanelWithLabel("Date:", order.getConvertedOrderTime());
        JPanel userIdPanel = createMemberPanelWithLabel("User ID:", order.getUserId());
        String summaryString = order.getItemsToString();
        summaryString = "<html>" + summaryString.replace(System.lineSeparator(), "<br/>") + "</html>";
        JPanel summaryPanel = createMemberPanelWithLabel("Summary:", summaryString);
        fields.add(orderIdPanel);
        fields.add(Box.createRigidArea(new Dimension(0, 20)));
        fields.add(datePanel);
        fields.add(Box.createRigidArea(new Dimension(0, 20)));
        fields.add(userIdPanel);
        fields.add(Box.createRigidArea(new Dimension(0, 20)));
        fields.add(summaryPanel);

        JLabel label = createTitle("Order Info");
        // Lay out the label and scroll pane from top to bottom.
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.PAGE_AXIS));

        detailsPanel.add(label);
        detailsPanel.add(Box.createRigidArea(new Dimension(0,5)));
        detailsPanel.add(fields);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Lay out the sections from left to right.
        createItemsTable();
        JSplitPane splitPane = new JSplitPane();
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        splitPane.setLeftComponent(detailsPanel);
        splitPane.setRightComponent(tableManager.getMainPanel());
        splitPane.setDividerLocation((int)(getPreferredSize().width * 0.25));
        splitPane.setOpaque(true);
        splitPane.setContinuousLayout(true);


        JButton buttonPanel = createButton("Close", e -> SwingUtilities.invokeLater(() -> {
            this.setVisible(false);
            this.dispose();
        }), true);
        this.getRootPane().setDefaultButton(buttonPanel);

        // Lay out the label and scroll pane from top to bottom.
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        CustomGridBagConstraints constraints = new CustomGridBagConstraints();
        contentPane.add(splitPane, constraints.constrainFillBoth().nextY());
        contentPane.add(buttonPanel, constraints.constrainLabel().nextY());

        setContentPane(contentPane);

        pack();
        Controls.centerDialog(this);

        return this;
    }

    private void createItemsTable() {
        List<ItemColumn> itemCols = Arrays.asList(ItemColumn.Image, ItemColumn.Name, ItemColumn.Description, ItemColumn.Price, ItemColumn.Count);
        tableModelList = new ArrayList<>();
        TableConfig tableConfig = TableConfig.create().withLinesInRow(6).withEditable(false).withBorder(true).withColumnReordering(true).withColumnResizing(false).build();
        tableManager = new TableManager<>(itemCols, tableModelList, tableConfig);
        refreshTable();
    }

    private void refreshTable() {
        // First clear the list and then add all items from shopping cart as view info models
        tableModelList.clear();

        List<Item> items = orderItems.getItems();

        items.forEach(item -> tableModelList.add(warehouseItemToItemViewInfo(itemToWarehouseItem(item))));
        try {
            tableManager.refresh();
        } catch (Throwable t) {
            logger.error("Error has occurred while trying to refresh table.", t);
        }
    }

    private  WarehouseItem itemToWarehouseItem(Item item) {
        return new WarehouseItem(item.getId(),item.getCategory(), item.getName(), item.getDescription(), item.getPrice(), item.getProfitPercent(), item.getDiscountPercent(), item.getCount());
    }

    private JPanel createMemberPanelWithLabel(String name, String value) {
        return createMemberPanel(name, value, 0, 0, true);
    }

    private JPanel createMemberPanelWithTextArea(String name, String value, int textAreaWidth, int textAreaHeight) {
        return createMemberPanel(name, value, textAreaWidth, textAreaHeight, false);
    }

    private JPanel createMemberPanelWithoutTextArea(String name) {
        return createMemberPanel(name, null, 0, 0, false);
    }

    private JPanel createMemberPanel(String name, String value, int textAreaWidth, int textAreaHeight, boolean isLabel) {
        JPanel memberPanel = new JPanel();
        memberPanel.setLayout(new BoxLayout(memberPanel, BoxLayout.Y_AXIS));
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.X_AXIS));
        JLabel label = createLabel(name, Fonts.PANEL_HEADING_FONT);
        label.setVerticalAlignment(SwingConstants.TOP);

        inner.add(label);
        inner.add(Box.createHorizontalGlue());
        memberPanel.add(inner);
        memberPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        if (value != null) {
            if (isLabel) {
                JLabel label2 = createLabel(value, Fonts.PLAIN_FONT);
                JPanel inner2 = new JPanel();
                inner2.setLayout(new BoxLayout(inner2, BoxLayout.X_AXIS));
                inner2.add(label2);
                inner2.add(Box.createHorizontalGlue());
                memberPanel.add(inner2);
            } else {
                memberPanel.add(withScrollPane(createTextArea(value, false), textAreaWidth, textAreaHeight));
            }
        } else {
            memberPanel.add(Box.createVerticalGlue());
        }

        return memberPanel;
    }
}