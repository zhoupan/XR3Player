/**
 * 
 */
package browsers;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;

import application.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import tools.InfoTool;

/**
 * @author GOXR3PLUS
 *
 */
public class WebBrowserController extends BorderPane {

    /** The logger. */
    private final Logger logger = Logger.getLogger(getClass().getName());

    //------------------------------------------------------------

    @FXML
    private TabPane tabPane;

    @FXML
    private JFXButton addTab;

    // -------------------------------------------------------------

    /**
     * Constructor
     */
    public WebBrowserController() {
	// ------------------------------------FXMLLOADER ----------------------------------------
	FXMLLoader loader = new FXMLLoader(getClass().getResource(InfoTool.FXMLS + "WebBrowserController.fxml"));
	loader.setController(this);
	loader.setRoot(this);

	try {
	    loader.load();
	} catch (IOException ex) {
	    logger.log(Level.SEVERE, "", ex);
	}
    }

    /**
     * Called as soon as .fxml is initialized [[SuppressWarningsSpartan]]
     */
    @FXML
    private void initialize() {

	//tabPane
	tabPane.getTabs().clear();
	createNewTab("coz");

	//addTab
	addTab.setOnAction(a -> {

	    //Check tabs number
	    if (tabPane.getTabs().size() >= 4) {
		JFXDialog dialog = new JFXDialog();
		//Show Message
		Alert alert = new Alert(AlertType.WARNING,
			"Currently only 4 tabs are allowed , for performance reasons... \n\n If you can hack it without decompiling the code i will give you 5$ dollars via paypal ;)");
		alert.initOwner(Main.window);
		alert.initOwner(Main.window);
		alert.showAndWait();

		return;
	    }

	    //Create
	    createNewTab();
	});

    }

    /**
     * Creates a new tab for the webbrowser ->Directing to a specific website [[SuppressWarningsSpartan]]
     * 
     * @param webSite
     */
    public void createNewTab(String... webSite) {
	//Add new Tab
	Tab tab = new Tab("");
	WebBrowserTabController webBrowserTab = new WebBrowserTabController(tab,webSite.length==0?null:webSite[0]);
	tab.setOnCloseRequest(c -> {

	    //Check the tabs number
	    if (tabPane.getTabs().size() == 1)
		createNewTab();

	    // Delete cache for navigate back
	    webBrowserTab.webEngine.load("about:blank");

	    //Delete cookies  Experimental!!! 
	    //java.net.CookieHandler.setDefault(new java.net.CookieManager())

	});

	//Add the tab
	tabPane.getTabs().add(tab);
	//System.out.println(Arrays.asList(webSite))

    }

}
