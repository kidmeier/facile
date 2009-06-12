(ns clj.facile.taglib.html
  (:use clj.facile))

(deftaglib jsf-html
  :commandButton "javax.faces.HtmlCommandButton",
  :commandLink "javax.faces.HtmlCommandLink",
  :dataTable "javax.faces.HtmlDataTable",
  :form "javax.faces.HtmlForm",
  :graphicImage "javax.faces.HtmlGraphicImage",
  :inputHidden "javax.faces.HtmlInputHidden",
  :inputSecret "javax.faces.HtmlInputSecret",
  :inputText "javax.faces.HtmlInputText",
  :inputTextarea "javax.faces.HtmlInputTextarea",
  :message "javax.faces.HtmlMessage",
  :messages "javax.faces.HtmlMessages",
  :outputFormat "javax.faces.HtmlOutputFormat",
  :outputLabel "javax.faces.HtmlOutputLabel",
  :outputLink "javax.faces.HtmlOutputLink",
  :outputText "javax.faces.HtmlOutputText",
  :panelGrid "javax.faces.HtmlPanelGrid",
  :panelGroup "javax.faces.HtmlPanelGroup",
  :selectBooleanCheckbox "javax.faces.HtmlSelectBooleanCheckbox",
  :selectManyCheckbox "javax.faces.HtmlSelectManyCheckbox",
  :selectManyListbox "javax.faces.HtmlSelectManyListbox",
  :selectManyMenu "javax.faces.HtmlSelectManyMenu",
  :selectOneListbox "javax.faces.HtmlSelectOneListbox",
  :selectOneMenu "javax.faces.HtmlSelectOneMenu",
  :selectOneRadio "javax.faces.HtmlSelectOneRadio")

