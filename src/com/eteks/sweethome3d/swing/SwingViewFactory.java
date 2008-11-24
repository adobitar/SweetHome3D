/*
 * SwingViewFactory.java 28 oct. 2008
 *
 * Copyright (c) 2008 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d.swing;

import com.eteks.sweethome3d.model.BackgroundImage;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.FurnitureCatalog;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.BackgroundImageWizardController;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.FurnitureCatalogController;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;
import com.eteks.sweethome3d.viewcontroller.HelpController;
import com.eteks.sweethome3d.viewcontroller.HelpView;
import com.eteks.sweethome3d.viewcontroller.Home3DAttributesController;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.HomeController3D;
import com.eteks.sweethome3d.viewcontroller.HomeFurnitureController;
import com.eteks.sweethome3d.viewcontroller.HomeView;
import com.eteks.sweethome3d.viewcontroller.ImportedFurnitureWizardController;
import com.eteks.sweethome3d.viewcontroller.ImportedFurnitureWizardStepsView;
import com.eteks.sweethome3d.viewcontroller.ImportedTextureWizardController;
import com.eteks.sweethome3d.viewcontroller.PageSetupController;
import com.eteks.sweethome3d.viewcontroller.PlanController;
import com.eteks.sweethome3d.viewcontroller.PlanView;
import com.eteks.sweethome3d.viewcontroller.PrintPreviewController;
import com.eteks.sweethome3d.viewcontroller.RoomController;
import com.eteks.sweethome3d.viewcontroller.TextureChoiceController;
import com.eteks.sweethome3d.viewcontroller.TextureChoiceView;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskView;
import com.eteks.sweethome3d.viewcontroller.UserPreferencesController;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import com.eteks.sweethome3d.viewcontroller.WallController;
import com.eteks.sweethome3d.viewcontroller.WizardController;

/**
 * View factory that instantiates the Swing components of this package.
 * @author Emmanuel Puybaret
 */
public class SwingViewFactory implements ViewFactory {
  /**
   * Returns a new view that displays furniture <code>catalog</code>.
   */
  public View createFurnitureCatalogView(FurnitureCatalog catalog,
                                  UserPreferences preferences,
                                  FurnitureCatalogController furnitureCatalogController) {
    return new FurnitureCatalogTree(catalog, furnitureCatalogController);
  }
  
  /**
   * Returns a new table that displays <code>home</code> furniture.
   */
  public View createFurnitureView(Home home, UserPreferences preferences,
                                  FurnitureController furnitureController) {
    return new FurnitureTable(home, preferences, furnitureController);
  }

  /**
   * Returns a new view that displays <code>home</code> plan.
   */
  public PlanView createPlanView(Home home, UserPreferences preferences,
                                 PlanController planController) {
    return new PlanComponent(home, preferences, planController);
  }

  /**
   * Returns a new view that displays <code>home</code> in 3D.
   */
  public View createView3D(Home home, UserPreferences preferences,
                           HomeController3D homeController3D) {
    return new HomeComponent3D(home, homeController3D);
  }

  /**
   * Returns a new view that displays <code>home</code> and its sub views.
   */
  public HomeView createHomeView(Home home, UserPreferences preferences,
                                 HomeController homeController) {
    return new HomePane(home, preferences, homeController);
  }

  /**
   * Returns a new view that displays a wizard. 
   */
  public DialogView createWizardView(WizardController wizardController) {
    return new WizardPane(wizardController);
  }

  /**
   * Returns a new view that displays the different steps that helps user to choose a background image. 
   */
  public View createBackgroundImageWizardStepsView(BackgroundImage backgroundImage,
                      UserPreferences preferences, 
                      BackgroundImageWizardController backgroundImageWizardController) {
    return new BackgroundImageWizardStepsPanel(backgroundImage, preferences,  
        backgroundImageWizardController);
  }

  /**
   * Returns a new view that displays the different steps that helps user to import furniture. 
   */
  public ImportedFurnitureWizardStepsView createImportedFurnitureWizardStepsView(
                      CatalogPieceOfFurniture piece,
                      String modelName, boolean importHomePiece,
                      UserPreferences preferences, 
                      ImportedFurnitureWizardController importedFurnitureWizardController) {
    return new ImportedFurnitureWizardStepsPanel(piece, modelName, importHomePiece,
        preferences, importedFurnitureWizardController);
  }

  /**
   * Returns a new view that displays the different steps that helps the user to import a texture. 
   */
  public View createImportedTextureWizardStepsView(
                      CatalogTexture texture, String textureName,
                      UserPreferences preferences,
                      ImportedTextureWizardController importedTextureWizardController) {
    return new ImportedTextureWizardStepsPanel(texture, textureName, preferences,
        importedTextureWizardController);
  }

  /**
   * Returns a new view that displays message for a threaded task.
   */
  public ThreadedTaskView createThreadedTaskView(String taskMessage,
                                                 ThreadedTaskController threadedTaskController) {
    return new ThreadedTaskPanel(taskMessage, threadedTaskController);
  }

  /**
   * Returns a new view that edits user preferences.
   */
  public DialogView createUserPreferencesView(UserPreferences preferences,
                                          UserPreferencesController userPreferencesController) {
    return new UserPreferencesPanel(preferences, userPreferencesController);
  }
  
  /**
   * Returns a new view that edits the selected furniture in <code>home</code>.
   */
  public DialogView createHomeFurnitureView(UserPreferences preferences,
                               HomeFurnitureController homeFurnitureController) {
    return new HomeFurniturePanel(preferences, homeFurnitureController);
  }

  /**
   * Returns a new view that edits wall values.
   */
  public DialogView createWallView(UserPreferences preferences,
                                 WallController wallController) {
    return new WallPanel(preferences, wallController);
  }
  
  /**
   * Returns a new view that edits room values.
   */
  public DialogView createRoomView(UserPreferences preferences,
                                   RoomController roomController) {
    return new RoomPanel(preferences, roomController);
  }
  
  /**
   * Returns a new view that edits 3D attributes.
   */
  public DialogView createHome3DAttributesView(UserPreferences preferences,
                                  Home3DAttributesController home3DAttributesController) {
    return new Home3DAttributesPanel(preferences, home3DAttributesController);    
  }
  
  /**
   * Returns a new view that edits the texture of the given controller.  
   */
  public TextureChoiceView createTextureChoiceView(UserPreferences preferences,
                                            TextureChoiceController textureChoiceController) {
    return new TextureChoiceComponent(preferences, textureChoiceController);
  }

  /**
   * Creates a new view that edits page setup.
   */
  public DialogView createPageSetupView(UserPreferences preferences,
                                           PageSetupController pageSetupController) {
    return new PageSetupPanel(pageSetupController);
  }

  /**
   * Returns a new view that displays <code>home</code> print preview. 
   */
  public DialogView createPrintPreviewView(Home home,
                                           HomeController homeController,
                                           PrintPreviewController printPreviewController) {
    return new PrintPreviewPanel(home, homeController, printPreviewController);
  }
  
  /**
   * Returns a new view that displays Sweet Home 3D help.
   */
  public HelpView createHelpView(UserPreferences preferences,
                                 HelpController helpController) {
    return new HelpPane(preferences, helpController);
  }
}
