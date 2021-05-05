package com.github.wangji92.arthas.plugin.ui;

import com.github.wangji92.arthas.plugin.common.combox.CustomComboBoxItem;
import com.github.wangji92.arthas.plugin.common.combox.CustomDefaultListCellRenderer;
import com.github.wangji92.arthas.plugin.common.enums.ShellScriptCommandEnum;
import com.github.wangji92.arthas.plugin.common.enums.ShellScriptConstantEnum;
import com.github.wangji92.arthas.plugin.common.enums.ShellScriptVariableEnum;
import com.github.wangji92.arthas.plugin.constants.ArthasCommandConstants;
import com.github.wangji92.arthas.plugin.setting.AppSettingsState;
import com.github.wangji92.arthas.plugin.utils.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;

import javax.swing.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 快捷命令
 *
 * @author 汪小哥
 * @date 05-05-2021
 */
public class ArthasShellScriptCommandDialog extends JDialog {
    private JPanel contentPane;
    private JComboBox shellScriptComboBox;
    private JButton shellScriptCommandButton;
    /**
     * 通用脚本
     */
    private JComboBox commonShellScriptComboBox;
    /**
     * 通用脚本点击
     */
    private JButton commonShellScriptCommandButton;
    private JButton closeScriptButton;
    private JRadioButton selectCommandCloseDialogRadioButton;
    private JButton dyCopyCommandButton;
    private JButton commonCopyCommandButton;

    private Project project;

    private String className;

    private String fieldName;
    private String methodName;

    private String executeInfo;

    private boolean modifierStatic;

    /**
     * 当前执行上下文
     */
    private Map<String, String> contextParams = new HashMap<>(10);

    public ArthasShellScriptCommandDialog(Project project, String className, String fieldName, String methodName, String executeInfo, boolean modifierStatic) {
        this.className = className;
        this.project = project;
        this.fieldName = fieldName;
        this.methodName = methodName;
        this.executeInfo = executeInfo;
        this.modifierStatic = modifierStatic;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(closeScriptButton);
        closeScriptButton.addActionListener(e -> onOK());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        initContextParam();

        init();
    }

    /**
     * 初始化上下文信息
     */
    private void initContextParam() {
        AppSettingsState instance = AppSettingsState.getInstance(project);
        Map<String, String> params = new HashMap<>(10);
        params.put(ShellScriptVariableEnum.PROPERTY_DEPTH.getEnumMsg(), instance.depthPrintProperty);
        params.put(ShellScriptVariableEnum.CLASS_NAME.getEnumMsg(), this.className);
        params.put(ShellScriptVariableEnum.METHOD_NAME.getEnumMsg(), this.methodName);
        params.put(ShellScriptVariableEnum.FIELD_NAME.getEnumMsg(), this.fieldName);
        params.put(ShellScriptVariableEnum.SPRING_CONTEXT.getEnumMsg(), instance.staticSpringContextOgnl);
        params.put(ShellScriptVariableEnum.INVOKE_COUNT.getEnumMsg(), instance.invokeCount);
        params.put(ShellScriptVariableEnum.INVOKE_MONITOR_COUNT.getEnumMsg(), instance.invokeMonitorCount);
        params.put(ShellScriptVariableEnum.INVOKE_MONITOR_INTERVAL.getEnumMsg(), instance.invokeMonitorInterval);
        String skpJdkMethodCommand = instance.traceSkipJdk ? "" : ArthasCommandConstants.DEFAULT_SKIP_JDK_FALSE;
        params.put(ShellScriptVariableEnum.SKIP_JDK_METHOD.getEnumMsg(), skpJdkMethodCommand);
        String printConditionExpress = instance.printConditionExpress ? "-v" : "";
        params.put(ShellScriptVariableEnum.PRINT_CONDITION_RESULT.getEnumMsg(), printConditionExpress);
        params.put(ShellScriptVariableEnum.EXECUTE_INFO.getEnumMsg(), this.executeInfo);
        params.put(ShellScriptVariableEnum.CLASSLOADER_HASH_VALUE.getEnumMsg(), "${CLASSLOADER_HASH_VALUE}");
        String methodNameNotStar = "*".equals(this.methodName) ? "" : this.methodName;
        params.put(ShellScriptVariableEnum.METHOD_NAME_NOT_STAR.getEnumMsg(), methodNameNotStar);
        String conditionExpressDisplay = instance.conditionExpressDisplay ? ArthasCommandConstants.DEFAULT_CONDITION_EXPRESS : "";
        params.put(ShellScriptVariableEnum.CONDITION_EXPRESS_DEFAULT.getEnumMsg(), conditionExpressDisplay);
        this.contextParams = params;
    }

    @SuppressWarnings("unchecked")
    private void init() {
        this.initDynamicShellScript();
        this.commonScriptInit();
        this.initSelectCommandCloseDialogSetting();
    }

    private void initSelectCommandCloseDialogSetting() {
        selectCommandCloseDialogRadioButton.addItemListener(e -> {
            if (e.getSource().equals(selectCommandCloseDialogRadioButton)) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    PropertiesComponentUtils.setValue("scriptDialogCloseWhenSelectedCommand", "n");
                } else if (e.getStateChange() == ItemEvent.SELECTED) {
                    PropertiesComponentUtils.setValue("scriptDialogCloseWhenSelectedCommand", "y");
                }
            }
        });
        AppSettingsState instance = AppSettingsState.getInstance(project);
        if ("y".equalsIgnoreCase(instance.scriptDialogCloseWhenSelectedCommand)) {
            selectCommandCloseDialogRadioButton.setSelected(true);
        }
    }

    /**
     * 初始化动态执行脚本
     */
    @SuppressWarnings("unchecked")
    private void initDynamicShellScript() {
        shellScriptCommandButton.addActionListener(e -> {
            Object selectedItem = shellScriptComboBox.getSelectedItem();
            assert selectedItem != null;
            String selectedItemStr = selectedItem.toString();
            String scCommand = "";
            if (selectedItemStr.contains(ShellScriptVariableEnum.CLASSLOADER_HASH_VALUE.getCode())) {
                scCommand = String.join(" ", "sc", "-d", this.className);
            }
            // 这里再次处理一下上下文信息
            String finalStr = StringUtils.stringSubstitutorFromText(selectedItemStr, contextParams);
            if (StringUtils.isNotBlank(finalStr)) {
                CommonExecuteScriptUtils.executeCommonScript(project, scCommand, finalStr, "");
            }
            this.doCloseDialog();
        });

        dyCopyCommandButton.addActionListener(e -> {
            Object selectedItem = shellScriptComboBox.getSelectedItem();
            assert selectedItem != null;
            String selectedItemStr = selectedItem.toString();
            ClipboardUtils.setClipboardString(selectedItemStr);
            if (selectedItemStr.contains(ShellScriptVariableEnum.CLASSLOADER_HASH_VALUE.getCode())) {
                NotifyUtils.notifyMessage(project, "命令已复制到剪切板,部分命令需要classloader hash value 直接执行不可以");
            } else {
                NotifyUtils.notifyMessage(project, "命令已复制到剪切板,到服务启动arthas 粘贴执行");
            }


        });
        shellScriptComboBox.setRenderer(new CustomDefaultListCellRenderer(shellScriptComboBox));
        for (ShellScriptCommandEnum shellScript : ShellScriptCommandEnum.values()) {
            if (Boolean.TRUE.equals(shellScript.getNeedClass()) && StringUtils.isBlank(this.className)) {
                continue;
            }
            if (Boolean.TRUE.equals(shellScript.getNeedField()) && StringUtils.isBlank(this.fieldName)) {
                continue;
            }
            if (Boolean.TRUE.equals(shellScript.getNeedMethod()) && (StringUtils.isBlank(this.methodName) || "*".equalsIgnoreCase(this.methodName))) {
                continue;
            }
            if (Boolean.TRUE.equals(shellScript.getNeedStatic()) && Boolean.FALSE.equals(this.modifierStatic)) {
                continue;
            }
            if (Boolean.FALSE.equals(shellScript.getNeedStatic()) && Boolean.TRUE.equals(this.modifierStatic)) {
                continue;
            }
            String codeValue = shellScript.getCode();
            String displayCode = StringUtils.stringSubstitutorFromText(codeValue, contextParams);
            CustomComboBoxItem<ShellScriptCommandEnum> boxItem = new CustomComboBoxItem<ShellScriptCommandEnum>();
            boxItem.setContentObject(shellScript);
            boxItem.setDisplay(displayCode);
            boxItem.setTipText(shellScript.getEnumMsg());
            shellScriptComboBox.addItem(boxItem);
        }
    }

    /**
     * 关闭窗口
     */
    private void doCloseDialog() {
        AppSettingsState instance = AppSettingsState.getInstance(project);
        if ("y".equalsIgnoreCase(instance.scriptDialogCloseWhenSelectedCommand)) {
            dispose();
        }
    }

    /**
     * 常用脚本添加
     */
    @SuppressWarnings("unchecked")
    private void commonScriptInit() {
        for (ShellScriptConstantEnum scriptConstantEnum : ShellScriptConstantEnum.values()) {
            CustomComboBoxItem<ShellScriptConstantEnum> boxItem = new CustomComboBoxItem<ShellScriptConstantEnum>();
            boxItem.setContentObject(scriptConstantEnum);
            boxItem.setDisplay(scriptConstantEnum.getCode());
            boxItem.setTipText(scriptConstantEnum.getEnumMsg());
            commonShellScriptComboBox.addItem(boxItem);
        }
        commonShellScriptComboBox.setRenderer(new CustomDefaultListCellRenderer(commonShellScriptComboBox));
        commonShellScriptCommandButton.addActionListener(e -> {
            Object selectedItem = commonShellScriptComboBox.getSelectedItem();
            assert selectedItem != null;
            String selectedItemStr = selectedItem.toString();
            if (StringUtils.isNotBlank(selectedItemStr)) {
                CommonExecuteScriptUtils.executeCommonScript(project, "", selectedItemStr, "");
            }
            this.doCloseDialog();
        });
        commonCopyCommandButton.addActionListener(e -> {
            Object selectedItem = commonShellScriptComboBox.getSelectedItem();
            assert selectedItem != null;
            String selectedItemStr = selectedItem.toString();
            ClipboardUtils.setClipboardString(selectedItemStr);
            if (selectedItemStr.contains(ShellScriptVariableEnum.CLASSLOADER_HASH_VALUE.getCode())) {
                NotifyUtils.notifyMessage(project, "命令已复制到剪切板,部分命令需要classloader hash value 直接执行不可以");
            } else {
                NotifyUtils.notifyMessage(project, "命令已复制到剪切板,到服务启动arthas 粘贴执行（部分为批量脚本不能执行，需手动修改)");
            }


        });
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    /**
     * 打开窗口
     */
    public void open(String title) {
        setTitle(title);
        pack();
        //两个屏幕处理出现问题，跳到主屏幕去了
        setLocationRelativeTo(WindowManager.getInstance().getFrame(this.project));
        setVisible(true);

    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getExecuteInfo() {
        return executeInfo;
    }

    public void setExecuteInfo(String executeInfo) {
        this.executeInfo = executeInfo;
    }
}
