/*
 * 
 */
package main.java.com.goxr3plus.xr3player.smartcontroller.presenter;

import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.controlsfx.control.textfield.TextFields;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import main.java.com.goxr3plus.xr3player.application.Main;
import main.java.com.goxr3plus.xr3player.application.modes.librarymode.Library;
import main.java.com.goxr3plus.xr3player.smartcontroller.enums.Genre;
import main.java.com.goxr3plus.xr3player.smartcontroller.media.Audio;
import main.java.com.goxr3plus.xr3player.smartcontroller.media.Media;

/**
 * WARNING! I WAS DRUNK WHEN WRITING THIS CLASS ;) , REALLY! FUCKED UP XOAXOAOXOAO
 * 
 * This class is used as a search Box for SmartController.
 *
 * @author GOXR3PLUS
 */
public class SmartControllerSearcher extends HBox {
	
	/** The search field. */
	private final TextField searchField = TextFields.createClearableTextField();
	
	/** The controller. */
	// Variables
	private SmartController smartController;
	
	/** The service. */
	private SearchService service = new SearchService();
	
	private boolean saveSettingBeforeSearch = true;
	
	/**
	 * Constructor.
	 *
	 * @param smartController
	 *            the control
	 */
	public SmartControllerSearcher(SmartController smartController) {
		this.smartController = smartController;
		
		//Super
		setAlignment(Pos.CENTER);
		getChildren().add(searchField);
		getStyleClass().add("search-box");
		
		// searchField
		searchField.setMinWidth(100);
		searchField.setPrefWidth(400);
		searchField.setMaxWidth(Integer.MAX_VALUE);
		searchField.setPromptText("Search.....");
		searchField.textProperty().addListener((observable , newValue , oldValue) -> {
			
			//Check if the controller is free
			if (!smartController.isFree(false))
				return;
			
			if (searchField.getText().isEmpty()) {
				saveSettingBeforeSearch = true;
				
				//Reset Aphabetical Bar Letter
				smartController.getAlphabetBar().setLetterPressed(false);
				
				//Reset the page number to the default before search
				if (service.getPaneNumberBeforeSearch() <= smartController.getMaximumList())
					smartController.currentPageProperty().set(service.getPaneNumberBeforeSearch());
				
				//continue 
				smartController.getNavigationHBox().setDisable(false);
				smartController.getLoadService().startService(false, false, false);
				
			} else if (Main.settingsWindow.getPlayListsSettingsController().getInstantSearch().isSelected()) {
				saveSettingsBeforeSearch();
				service.search();
			}
		});
		searchField.setOnKeyReleased(key -> {
			if (key.getCode() == KeyCode.ESCAPE)
				searchField.clear();
		});
		searchField.editableProperty().bind(service.runningProperty().not());
		//searchField.disableProperty().bind(Main.advancedSearch.showingProperty())
		searchField.setOnAction(ac -> reSearch());
		
		//Override the default context menu
		searchField.setContextMenu(new ContextMenu());
		
	}
	
	/**
	 * This method fires a search again based on currently given parameters
	 */
	public void reSearch() {
		if (!smartController.isFree(false))
			return;
		saveSettingsBeforeSearch();
		service.search();
	}
	
	/**
	 * //Save the Settings before the first search -> [ ScrollBar position and current page of the SmartController ]
	 */
	private void saveSettingsBeforeSearch() {
		
		//lock it so no override happens during the search
		if (!saveSettingBeforeSearch)
			return;
		
		service.pageBeforeSearch = smartController.currentPageProperty().get();
		smartController.getVerticalScrollBar().ifPresent(scrollBar -> smartController.setVerticalScrollValueWithoutSearch(scrollBar.getValue()));
		saveSettingBeforeSearch = false;
	}
	
	/**
	 * Returns true if the Search service is currently activated(if reset button is still visible).
	 *
	 * @return <b> True </b> if searchField is empty
	 */
	public boolean isActive() {
		return !searchField.getText().isEmpty();
	}
	
	/**
	 * @return the service
	 */
	public SearchService getService() {
		return service;
	}
	
	/**
	 * @return the searchField
	 */
	public TextField getSearchField() {
		return searchField;
	}
	
	/**
	 * The Class SearchService.
	 */
	public class SearchService extends Service<Void> {
		
		/** The word. */
		private String word;
		
		/** The counter. */
		int counter = 0;
		
		/** The page before search. */
		int pageBeforeSearch = 0;
		
		/**
		 * Constructor.
		 */
		public SearchService() {
			setOnSucceeded(s -> done());
			setOnFailed(f -> {
				smartController.getNavigationHBox().setDisable(false);
				done();
			});
			
		}
		
		/**
		 * You can start the search Service by calling this method.
		 */
		public void search() {
			// advanced search?
			// if (Main.advancedSearch.isShowing())
			// word = Main.advancedSearch.getTextForSearching();
			// else
			word = getSearchField().getText();
			smartController.getIndicatorVBox().visibleProperty().bind(runningProperty());
			smartController.getIndicator().progressProperty().bind(progressProperty());
			smartController.getCancelButton().setText("Searching...");
			smartController.getInformationTextArea().setText("\n Searching ....");
			smartController.getNavigationHBox().setDisable(true);
			
			//Clear the list
			smartController.getItemsObservableList().clear();
			
			reset();
			start();
		}
		
		/**
		 * When the Service is done.
		 */
		private void done() {
			smartController.updateList();
			smartController.unbind();
		}
		
		/**
		 * Returns the page that the SmartController was before the Search.
		 *
		 * @return the pane number before search
		 */
		private int getPaneNumberBeforeSearch() {
			return pageBeforeSearch;
		}
		
		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {
				/**
				 * [[SuppressWarningsSpartan]]
				 */
				@Override
				protected Void call() throws Exception {
					
					// Counter
					counter = 0;
					
					// if (word.isEmpty())
					//	word = "";
					
					// Given Work
					System.out.println("Searching for word:[" + word + "]");
					String query = "";
					
					//--------------SEARCH WINDOW SPECIAL SEARCH----------------------------
					
					if (smartController.getGenre() == Genre.SEARCHWINDOW) {
						
						//Let's create the UNION
						ArrayList<String> queryArray = new ArrayList<>();
						ObservableList<Library> observableList = Main.libraryMode.teamViewer.getViewer().getItemsObservableList();
						smartController.setTotalInDataBase(observableList.stream().mapToInt(Library::getTotalEntries).sum());
						
						//Check if any PlayLists exist
						if (observableList.isEmpty()) {
							return null;
						}
						
						queryArray.add("SELECT * FROM (");
						
						//For Each
						Main.libraryMode.teamViewer.getViewer().getItemsObservableList().forEach(lib -> {
							if (lib.getPosition() != observableList.size() - 1)
								queryArray.add(" SELECT * FROM '" + lib.getDataBaseTableName() + "' UNION ALL ");
							else
								queryArray.add(" SELECT * FROM '" + lib.getDataBaseTableName() + "' ");
						});
						
						//Choose the correct query based on the settings of the user
						if (Main.settingsWindow.getPlayListsSettingsController().getFileSearchGroup().getToggles().get(0).isSelected())
							queryArray.add(" ) WHERE PATH LIKE '%" + word + "%' GROUP BY PATH LIMIT " + smartController.getMaximumPerPage() + " ");
						else
							queryArray.add(" ) WHERE replace(path, rtrim(path, replace(path,'" + File.separator + "','')),'') LIKE '%" + word + "%' GROUP BY PATH LIMIT "
									+ smartController.getMaximumPerPage() + " ");
						
						query = String.join("", queryArray);
						//System.out.println(query)
						
					}
					
					//--------------NORMAL PLAYLISTS SEARCH----------------------------
					
					else {
						query = "SELECT * FROM '" + smartController.getDataBaseTableName() + "' ";
						
						String wordQuery = smartController.getAlphabetBar().isLetterPressed() ? word + "%" : ( "%" + word + "%" );
						
						//Choose the correct query based on the settings of the user
						if (Main.settingsWindow.getPlayListsSettingsController().getFileSearchGroup().getToggles().get(0).isSelected())
							query = query + " WHERE PATH LIKE '" + wordQuery + "' LIMIT " + smartController.getMaximumPerPage();
						else
							query = query + " WHERE replace(path, rtrim(path, replace(path, '" + File.separator + "', '')), '') LIKE '" + wordQuery + "' LIMIT "
									+ smartController.getMaximumPerPage();
					}
					
					//System.out.println(query);
					
					//Continue
					try (ResultSet resultSet = Main.dbManager.getConnection().createStatement().executeQuery(query)) {
						//try (ResultSet resultSet = Main.dbManager.connection1.createStatement().executeQuery(query)) {
						
						//Fetch the items from the database
						Platform.runLater(() -> smartController.getCancelButton().setText("Adding data..."));
						List<Media> arrayList = new ArrayList<>();
						while (resultSet.next()) {
							//Add the Audio to the ArrayList
							arrayList.add(new Audio(resultSet.getString("PATH"), resultSet.getDouble("STARS"), resultSet.getInt("TIMESPLAYED"), resultSet.getString("DATE"),
									resultSet.getString("HOUR"), smartController.getGenre(), arrayList.size() + 1));
							
							updateProgress(++counter, smartController.getMaximumPerPage());
						}
						
						//Add the the items to the observable list
						CountDownLatch countDown = new CountDownLatch(1);
						Platform.runLater(() -> {
							smartController.getItemsObservableList().addAll(arrayList);
							smartController.getNormalModeMediatTableViewer().getAllDetailsService().restartService(smartController.getNormalModeMediatTableViewer());
							countDown.countDown();
						});
						countDown.await();
						
					} catch (Exception ex) {
						Main.logger.log(Level.WARNING, "", ex);
					}
					
					return null;
				}
				
			};
		}
		
	}
	
	//	/**
	//	 * This class contains more advanced search features.
	//	 *
	//	 * @author GOXR3PLUS
	//	 */
	//	public static class AdvancedSearch extends BorderPane {
	//		
	//		/** The search field. */
	//		@FXML
	//		private JFXTextField searchField;
	//		
	//		/** The search on fly. */
	//		@FXML
	//		private JFXToggleButton searchOnFly;
	//		
	//		/** The case sensitive. */
	//		@FXML
	//		private JFXCheckBox caseSensitive;
	//		
	//		/** The bottom V box. */
	//		@FXML
	//		private VBox bottomVBox;
	//		
	//		/** The pop over. */
	//		private PopOver popOver = new PopOver();
	//		
	//		/** The controller. */
	//		SmartController controller;
	//		
	//		/**
	//		 * Constructor.
	//		 */
	//		public AdvancedSearch() {
	//			
	//			// Load the f x m l file
	//			FXMLLoader loader = new FXMLLoader(getClass().getResource(InfoTool.FXMLS + "SearchSettings.fxml"));
	//			loader.setController(this);
	//			loader.setRoot(this);
	//			
	//			try {
	//				loader.load();
	//			} catch (IOException ex) {
	//				Main.logger.log(Level.WARNING, "", ex);
	//			}
	//			
	//		}
	//		
	//		/**
	//		 * Called as soon as .fxml has been initialized
	//		 */
	//		@FXML
	//		private void initialize() {
	//			
	//			// TagsBar
	//			//			TagsBar tagsBar = new TagsBar();
	//			//			tagsBar.setMaxWidth(InfoTool.getScreenWidth() / 2.5);
	//			//			tagsBar.getEntries().addAll(RadioStationsController.musicGenres);
	//			//			bottomVBox.getChildren().add(tagsBar);
	//			
	//			// PopOver
	//			popOver.setContentNode(this);
	//			popOver.getScene().getStylesheets().add(getClass().getResource(InfoTool.STYLES + InfoTool.APPLICATIONCSS).toExternalForm());
	//			popOver.setDetachable(false);
	//			popOver.setAutoHide(true);
	//			popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
	//			popOver.setOnHidden(h -> controller.getSearchService().getSearchField().textProperty().unbind());
	//			
	//			// this
	//			setOnMouseEntered(m -> requestFocus());
	//			
	//			searchField.setContextMenu(new ContextMenu());
	//			
	//		}
	//		
	//		/**
	//		 * Search on fly selected.
	//		 *
	//		 * @return true, if successful
	//		 */
	//		public boolean searchOnFlySelected() {
	//			return searchOnFly.isSelected();
	//		}
	//		
	//		/**
	//		 * Shows the Advanced Search with the given parameters.
	//		 *
	//		 * @param node
	//		 *            the node
	//		 * @param controller
	//		 *            the controller
	//		 */
	//		public void show(Node node , SmartController controller) {
	//			searchField.editableProperty().bind(controller.getSearchService().getService().runningProperty().not());
	//			this.controller = controller;
	//			searchField.setText(this.controller.getSearchService().getSearchField().getText());
	//			this.controller.getSearchService().getSearchField().textProperty().bind(searchField.textProperty());
	//			
	//			// Find the correct arrow location
	//			double width = popOver.getWidth();
	//			double height = popOver.getHeight();
	//			Bounds bounds = controller.getSearchService().getSearchField().localToScreen(controller.getSearchService().getSearchField().getBoundsInLocal());
	//			boolean fitOnTop = bounds.getMinY() - height > 0; // top?
	//			boolean fitOnLeft = bounds.getMinX() - width > 0; // left?
	//			boolean fitOnRight = bounds.getMaxX() + width < InfoTool.getScreenWidth();// right?
	//			boolean fitOnBottom = bounds.getMaxY() + height < InfoTool.getScreenHeight(); // bottom?
	//			
	//			if (fitOnTop)
	//				popOver.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
	//			else if (fitOnBottom)
	//				popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
	//			else if (fitOnLeft)
	//				popOver.setArrowLocation(ArrowLocation.RIGHT_CENTER);
	//			else if (fitOnRight)
	//				popOver.setArrowLocation(ArrowLocation.LEFT_CENTER);
	//			
	//			popOver.show(node);
	//			searchField.requestFocus();
	//		}
	//		
	//		/**
	//		 * Checks if is showing.
	//		 *
	//		 * @return true, if is showing
	//		 */
	//		public boolean isShowing() {
	//			return popOver.isShowing();
	//		}
	//		
	//		/**
	//		 * Showing property.
	//		 *
	//		 * @return the read only boolean property
	//		 */
	//		public ReadOnlyBooleanProperty showingProperty() {
	//			return popOver.showingProperty();
	//		}
	//		
	//		/**
	//		 * Gets the text for searching.
	//		 *
	//		 * @return the text for searching
	//		 */
	//		public String getTextForSearching() {
	//			return searchField.getText();
	//		}
	//		
	//		/**
	//		 * Hide.
	//		 */
	//		public void hide() {
	//			popOver.hide();
	//		}
	//		
	//	}
	
}
