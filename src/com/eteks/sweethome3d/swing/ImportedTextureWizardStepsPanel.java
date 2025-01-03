/*
 * ImportedTextureWizardStepsPanel.java 01 oct. 2008
 *
 * Sweet Home 3D, Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.LengthUnit;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.tools.TemporaryURLContent;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.ImportedTextureWizardController;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * Wizard panel for background image choice.
 * @author Emmanuel Puybaret
 */
public class ImportedTextureWizardStepsPanel extends JPanel implements View {
  private static final int LARGE_IMAGE_PIXEL_COUNT_THRESHOLD = 640 * 640;
  private static final int IMAGE_PREFERRED_MAX_SIZE = 512;
  private static final int LARGE_IMAGE_MAX_PIXEL_COUNT = IMAGE_PREFERRED_MAX_SIZE * IMAGE_PREFERRED_MAX_SIZE;

  private static final int IMAGE_PREFERRED_SIZE = Math.round(150 * SwingTools.getResolutionScale());

  private final ImportedTextureWizardController controller;
  private CardLayout                      cardLayout;
  private JLabel                          imageChoiceOrChangeLabel;
  private JButton                         imageChoiceOrChangeButton;
  private JButton                         findImagesButton;
  private JLabel                          imageChoiceErrorLabel;
  private ScaledImageComponent            imageChoicePreviewComponent;
  private JLabel                          attributesLabel;
  private JLabel                          nameLabel;
  private JTextField                      nameTextField;
  private JLabel                          categoryLabel;
  private JComboBox                       categoryComboBox;
  private JLabel                          creatorLabel;
  private JTextField                      creatorTextField;
  private JLabel                          widthLabel;
  private JSpinner                        widthSpinner;
  private JLabel                          heightLabel;
  private JSpinner                        heightSpinner;
  private ScaledImageComponent            attributesPreviewComponent;
  private Executor                        imageLoader;
  private static BufferedImage            waitImage;

  /**
   * Creates a view for texture image choice and attributes.
   */
  public ImportedTextureWizardStepsPanel(CatalogTexture catalogTexture,
                                         String textureName,
                                         UserPreferences preferences,
                                         final ImportedTextureWizardController controller) {
    this.controller = controller;
    this.imageLoader = Executors.newSingleThreadExecutor();
    createComponents(preferences, controller);
    setMnemonics(preferences);
    layoutComponents();
    updateController(catalogTexture, preferences);
    if (textureName != null) {
      updateController(textureName, controller.getContentManager(), preferences, true);
    }

    controller.addPropertyChangeListener(ImportedTextureWizardController.Property.STEP,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            updateStep(controller);
          }
        });
  }

  /**
   * Creates components displayed by this panel.
   */
  private void createComponents(final UserPreferences preferences,
                                final ImportedTextureWizardController controller) {
    // Get unit name matching current unit
    String unitName = preferences.getLengthUnit().getName();

    // Image choice panel components
    this.imageChoiceOrChangeLabel = new JLabel();
    this.imageChoiceOrChangeButton = new JButton();
    this.imageChoiceOrChangeButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          String imageName = showImageChoiceDialog(preferences, controller.getContentManager());
          if (imageName != null) {
            updateController(imageName, controller.getContentManager(), preferences, false);
          }
        }
      });
    try {
      this.findImagesButton = new JButton(SwingTools.getLocalizedLabelText(preferences,
          ImportedTextureWizardStepsPanel.class, "findImagesButton.text"));
      final String findImagesUrl = preferences.getLocalizedString(
          ImportedTextureWizardStepsPanel.class, "findImagesButton.url");
      this.findImagesButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            boolean documentShown = false;
            try {
              // Display Find models page in browser
              documentShown = SwingTools.showDocumentInBrowser(new URL(findImagesUrl));
            } catch (MalformedURLException ex) {
              // Document isn't shown
            }
            if (!documentShown) {
              // If the document wasn't shown, display a message
              // with a copiable URL in a message box
              JTextArea findImagesMessageTextArea = new JTextArea(preferences.getLocalizedString(
                  ImportedTextureWizardStepsPanel.class, "findImagesMessage.text"));
              String findImagesTitle = preferences.getLocalizedString(
                  ImportedTextureWizardStepsPanel.class, "findImagesMessage.title");
              findImagesMessageTextArea.setEditable(false);
              findImagesMessageTextArea.setOpaque(false);
              SwingTools.showMessageDialog(ImportedTextureWizardStepsPanel.this,
                  findImagesMessageTextArea, findImagesTitle,
                  JOptionPane.INFORMATION_MESSAGE);
            }
          }
        });
    } catch (IllegalArgumentException ex) {
      // Don't create findImagesButton if its text or url isn't defined
    }
    this.imageChoiceErrorLabel = new JLabel(preferences.getLocalizedString(
        ImportedTextureWizardStepsPanel.class, "imageChoiceErrorLabel.text"));
    // Make imageChoiceErrorLabel visible only if an error occurred during image content loading
    this.imageChoiceErrorLabel.setVisible(false);
    this.imageChoicePreviewComponent = new ScaledImageComponent();
    // Add a transfer handler to image preview component to let user drag and drop an image in component
    this.imageChoicePreviewComponent.setTransferHandler(new TransferHandler() {
        @Override
        public boolean canImport(JComponent comp, DataFlavor [] flavors) {
          return Arrays.asList(flavors).contains(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(JComponent comp, Transferable transferedFiles) {
          boolean success = false;
          try {
            List<File> files = (List<File>)transferedFiles.getTransferData(DataFlavor.javaFileListFlavor);
            for (File file : files) {
              final String textureName = file.getAbsolutePath();
              // Try to import the first file that would be accepted by content manager
              if (controller.getContentManager().isAcceptable(textureName, ContentManager.ContentType.IMAGE)) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                      updateController(textureName, controller.getContentManager(), preferences, false);
                    }
                  });
                success = true;
                break;
              }
            }
          } catch (UnsupportedFlavorException ex) {
            // No success
          } catch (IOException ex) {
            // No success
          }
          if (!success) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  JOptionPane.showMessageDialog(SwingUtilities.getRootPane(ImportedTextureWizardStepsPanel.this),
                      preferences.getLocalizedString(ImportedTextureWizardStepsPanel.class, "imageChoiceErrorLabel.text"));
                }
              });
          }
          return success;
        }
      });
    this.imageChoicePreviewComponent.setBorder(SwingTools.getDropableComponentBorder());

    // Attributes panel components
    this.attributesLabel = new JLabel(preferences.getLocalizedString(
        ImportedTextureWizardStepsPanel.class, "attributesLabel.text"));
    this.nameLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
        ImportedTextureWizardStepsPanel.class, "nameLabel.text"));
    this.nameTextField = new JTextField(10);
    if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
      SwingTools.addAutoSelectionOnFocusGain(this.nameTextField);
    }
    DocumentListener nameListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent ev) {
          nameTextField.getDocument().removeDocumentListener(this);
          controller.setName(nameTextField.getText().trim());
          nameTextField.getDocument().addDocumentListener(this);
        }

        public void insertUpdate(DocumentEvent ev) {
          changedUpdate(ev);
        }

        public void removeUpdate(DocumentEvent ev) {
          changedUpdate(ev);
        }
      };
    this.nameTextField.getDocument().addDocumentListener(nameListener);
    controller.addPropertyChangeListener(ImportedTextureWizardController.Property.NAME,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            // If name changes update name text field
            if (!nameTextField.getText().trim().equals(controller.getName())) {
              nameTextField.setText(controller.getName());
            }
          }
        });

    this.categoryLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
        ImportedTextureWizardStepsPanel.class, "categoryLabel.text"));
    this.categoryComboBox = new JComboBox(preferences.getTexturesCatalog().getCategories().toArray());
    this.categoryComboBox.setEditable(true);
    final ComboBoxEditor defaultEditor = this.categoryComboBox.getEditor();
    // Change editor to edit category name
    this.categoryComboBox.setEditor(new ComboBoxEditor() {
        public Object getItem() {
          String name = (String)defaultEditor.getItem();
          name = name.trim();
          // If category is empty, replace it by the last selected item
          if (name.length() == 0) {
            Object selectedItem = categoryComboBox.getSelectedItem();
            setItem(selectedItem);
            return selectedItem;
          } else {
            TexturesCategory category = new TexturesCategory(name);
            // Search an existing category
            List<TexturesCategory> categories = preferences.getTexturesCatalog().getCategories();
            int categoryIndex = Collections.binarySearch(categories, category);
            if (categoryIndex >= 0) {
              return categories.get(categoryIndex);
            }
            // If no existing category was found, return a new one
            return category;
          }
        }

        public void setItem(Object value) {
          if (value != null) {
            TexturesCategory category = (TexturesCategory)value;
            defaultEditor.setItem(category.getName());
          }
        }

        public void addActionListener(ActionListener l) {
          defaultEditor.addActionListener(l);
        }

        public Component getEditorComponent() {
          return defaultEditor.getEditorComponent();
        }

        public void removeActionListener(ActionListener l) {
          defaultEditor.removeActionListener(l);
        }

        public void selectAll() {
          defaultEditor.selectAll();
        }
      });
    this.categoryComboBox.setRenderer(new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
          TexturesCategory category = (TexturesCategory)value;
          return super.getListCellRendererComponent(list, category.getName(), index, isSelected, cellHasFocus);
        }
      });
    this.categoryComboBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ev) {
          controller.setCategory((TexturesCategory)ev.getItem());
        }
      });
    controller.addPropertyChangeListener(ImportedTextureWizardController.Property.CATEGORY,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            // If category changes update category combo box
            TexturesCategory category = controller.getCategory();
            if (category != null) {
              categoryComboBox.setSelectedItem(category);
            }
          }
        });

    this.creatorLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
        ImportedTextureWizardStepsPanel.class, "creatorLabel.text"));
    this.creatorTextField = new JTextField(10);
    if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
      SwingTools.addAutoSelectionOnFocusGain(this.creatorTextField);
    }
    DocumentListener creatorListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent ev) {
          creatorTextField.getDocument().removeDocumentListener(this);
          controller.setCreator(creatorTextField.getText().trim());
          creatorTextField.getDocument().addDocumentListener(this);
        }

        public void insertUpdate(DocumentEvent ev) {
          changedUpdate(ev);
        }

        public void removeUpdate(DocumentEvent ev) {
          changedUpdate(ev);
        }
      };
    this.creatorTextField.getDocument().addDocumentListener(creatorListener);
    controller.addPropertyChangeListener(ImportedTextureWizardController.Property.CREATOR,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            // If creator changes update creator text field
            if (!creatorTextField.getText().trim().equals(controller.getCreator())) {
              creatorTextField.setText(controller.getCreator());
            }
          }
        });

    this.widthLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
        ImportedTextureWizardStepsPanel.class, "widthLabel.text", unitName));
    float minimumLength = preferences.getLengthUnit().getMinimumLength();
    float maximumLength = preferences.getLengthUnit().getMaximumLength();
    final NullableSpinner.NullableSpinnerLengthModel widthSpinnerModel =
        new NullableSpinner.NullableSpinnerLengthModel(preferences, minimumLength, maximumLength);
    this.widthSpinner = new NullableSpinner(widthSpinnerModel);
    widthSpinnerModel.addChangeListener(new ChangeListener () {
        public void stateChanged(ChangeEvent ev) {
          widthSpinnerModel.removeChangeListener(this);
          // If width spinner value changes update controller
          controller.setWidth(widthSpinnerModel.getLength());
          widthSpinnerModel.addChangeListener(this);
        }
      });
    controller.addPropertyChangeListener(ImportedTextureWizardController.Property.WIDTH,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            // If width changes update width spinner
            widthSpinnerModel.setLength(controller.getWidth());
          }
        });

    this.heightLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
            ImportedTextureWizardStepsPanel.class, "heightLabel.text", unitName));
    final NullableSpinner.NullableSpinnerLengthModel heightSpinnerModel =
        new NullableSpinner.NullableSpinnerLengthModel(preferences, minimumLength, maximumLength);
    this.heightSpinner = new NullableSpinner(heightSpinnerModel);
    heightSpinnerModel.addChangeListener(new ChangeListener () {
        public void stateChanged(ChangeEvent ev) {
          heightSpinnerModel.removeChangeListener(this);
          // If width spinner value changes update controller
          controller.setHeight(heightSpinnerModel.getLength());
          heightSpinnerModel.addChangeListener(this);
        }
      });
    controller.addPropertyChangeListener(ImportedTextureWizardController.Property.HEIGHT,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            // If height changes update height spinner
            heightSpinnerModel.setLength(controller.getHeight());
          }
        });

    this.attributesPreviewComponent = new ScaledImageComponent();

    PropertyChangeListener imageAttributesListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          updateAttributesPreviewImage();
        }
      };
    controller.addPropertyChangeListener(ImportedTextureWizardController.Property.IMAGE, imageAttributesListener);
    controller.addPropertyChangeListener(ImportedTextureWizardController.Property.WIDTH, imageAttributesListener);
    controller.addPropertyChangeListener(ImportedTextureWizardController.Property.HEIGHT, imageAttributesListener);
  }

  /**
   * Sets components mnemonics and label / component associations.
   */
  private void setMnemonics(UserPreferences preferences) {
    if (!OperatingSystem.isMacOSX()) {
      if (this.findImagesButton != null) {
        this.findImagesButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            ImportedTextureWizardStepsPanel.class, "findImagesButton.mnemonic")).getKeyCode());
      }
      this.nameLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          ImportedTextureWizardStepsPanel.class, "nameLabel.mnemonic")).getKeyCode());
      this.nameLabel.setLabelFor(this.nameTextField);
      this.categoryLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          ImportedTextureWizardStepsPanel.class, "categoryLabel.mnemonic")).getKeyCode());
      this.categoryLabel.setLabelFor(this.categoryComboBox);
      this.creatorLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          ImportedTextureWizardStepsPanel.class, "creatorLabel.mnemonic")).getKeyCode());
      this.creatorLabel.setLabelFor(this.creatorTextField);
      this.widthLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          ImportedTextureWizardStepsPanel.class, "widthLabel.mnemonic")).getKeyCode());
      this.widthLabel.setLabelFor(this.widthSpinner);
      this.heightLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          ImportedTextureWizardStepsPanel.class, "heightLabel.mnemonic")).getKeyCode());
      this.heightLabel.setLabelFor(this.heightSpinner);
    }
  }

  /**
   * Layouts components in 3 panels added to this panel as cards.
   */
  private void layoutComponents() {
    this.cardLayout = new CardLayout();
    setLayout(this.cardLayout);
    int standardGap = Math.round(5 * SwingTools.getResolutionScale());
    JPanel imageChoiceTopPanel = new JPanel(new GridBagLayout());
    imageChoiceTopPanel.add(this.imageChoiceOrChangeLabel, new GridBagConstraints(
        0, 0, 2, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(standardGap, 0, standardGap, 0), 0, 0));
    this.imageChoicePreviewComponent.setPreferredSize(new Dimension(IMAGE_PREFERRED_SIZE, IMAGE_PREFERRED_SIZE));
    if (this.findImagesButton != null) {
      imageChoiceTopPanel.add(this.imageChoiceOrChangeButton, new GridBagConstraints(
          0, 1, 1, 1, 1, 0, GridBagConstraints.LINE_END,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
      imageChoiceTopPanel.add(this.findImagesButton, new GridBagConstraints(
          1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    } else {
      imageChoiceTopPanel.add(this.imageChoiceOrChangeButton, new GridBagConstraints(
          0, 1, 2, 1, 1, 0, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    }
    imageChoiceTopPanel.add(this.imageChoiceErrorLabel, new GridBagConstraints(
        0, 2, 2, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(standardGap, 0, 0, 0), 0, 0));

    JPanel imageChoicePanel = new JPanel(new ProportionalLayout());
    imageChoicePanel.add(imageChoiceTopPanel, ProportionalLayout.Constraints.TOP);
    imageChoicePanel.add(this.imageChoicePreviewComponent,
        ProportionalLayout.Constraints.BOTTOM);

    JPanel attributesPanel = new JPanel(new GridBagLayout());
    attributesPanel.add(this.attributesLabel, new GridBagConstraints(
        0, 0, 3, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(standardGap, 0, 10, 0), 0, 0));
    this.attributesPreviewComponent.setPreferredSize(new Dimension(IMAGE_PREFERRED_SIZE, IMAGE_PREFERRED_SIZE));
    attributesPanel.add(this.attributesPreviewComponent, new GridBagConstraints(
        0, 1, 1, 6, 1, 0, GridBagConstraints.NORTH,
        GridBagConstraints.NONE, new Insets(0, 0, standardGap, standardGap), 0, 0));
    attributesPanel.add(this.nameLabel, new GridBagConstraints(
        1, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 0, standardGap, standardGap), 0, 0));
    attributesPanel.add(this.nameTextField, new GridBagConstraints(
        2, 1, 1, 1, 0, 0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 0), 0, 0));
    attributesPanel.add(this.categoryLabel, new GridBagConstraints(
        1, 2, 1, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 0, standardGap, standardGap), 0, 0));
    attributesPanel.add(this.categoryComboBox, new GridBagConstraints(
        2, 2, 1, 1, 0, 0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 0), 0, 0));
    attributesPanel.add(this.creatorLabel, new GridBagConstraints(
        1, 3, 1, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 0, standardGap, standardGap), 0, 0));
    attributesPanel.add(this.creatorTextField, new GridBagConstraints(
        2, 3, 1, 1, 0, 0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 0), 0, 0));
    attributesPanel.add(this.widthLabel, new GridBagConstraints(
        1, 4, 1, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 0, standardGap, standardGap), 0, 0));
    attributesPanel.add(this.widthSpinner, new GridBagConstraints(
        2, 4, 1, 1, 0, 0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 0), 0, 0));
    attributesPanel.add(this.heightLabel, new GridBagConstraints(
        1, 5, 1, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 0, standardGap, standardGap), 0, 0));
    attributesPanel.add(this.heightSpinner, new GridBagConstraints(
        2, 5, 1, 1, 0, 0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 0), 0, 0));
    // Add a dummy label to force components to be at top of panel
    attributesPanel.add(new JLabel(), new GridBagConstraints(
        1, 6, 2, 1, 1, 1, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

    add(imageChoicePanel, ImportedTextureWizardController.Step.IMAGE.name());
    add(attributesPanel, ImportedTextureWizardController.Step.ATTRIBUTES.name());
  }

  /**
   * Switches to the component card matching current step.
   */
  public void updateStep(ImportedTextureWizardController controller) {
    ImportedTextureWizardController.Step step = controller.getStep();
    this.cardLayout.show(this, step.name());
    switch (step) {
      case IMAGE:
        this.imageChoiceOrChangeButton.requestFocusInWindow();
        break;
      case ATTRIBUTES:
        this.nameTextField.requestFocusInWindow();
        break;
    }
  }

  /**
   * Updates controller initial values from <code>textureImage</code>.
   */
  private void updateController(final CatalogTexture catalogTexture,
                                final UserPreferences preferences) {
    if (catalogTexture == null) {
      setImageChoiceTexts(preferences);
      updatePreviewComponentsImage(null);
    } else {
      setImageChangeTexts(preferences);
      // Read image in imageLoader executor
      this.imageLoader.execute(new Runnable() {
          public void run() {
            BufferedImage image = null;
            try {
              image = readImage(catalogTexture.getImage(), preferences);
            } catch (IOException ex) {
              // image is null
            }
            final BufferedImage readImage = image;
            // Update components in dispatch thread
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  if (readImage != null) {
                    controller.setImage(catalogTexture.getImage());
                    controller.setName(catalogTexture.getName());
                    controller.setCategory(catalogTexture.getCategory());
                    controller.setCreator(catalogTexture.getCreator());
                    controller.setWidth(catalogTexture.getWidth());
                    controller.setHeight(catalogTexture.getHeight());
                  } else {
                    controller.setImage(null);
                    setImageChoiceTexts(preferences);
                    imageChoiceErrorLabel.setVisible(true);
                  }
                }
              });

          }
        });
    }
  }

  /**
   * Reads image from <code>imageName</code> and updates controller values.
   */
  private void updateController(final String imageName,
                                final ContentManager contentManager,
                                final UserPreferences preferences,
                                final boolean ignoreException) {
    // Read image in imageLoader executor
    this.imageLoader.execute(new Runnable() {
        public void run() {
          Content imageContent = null;
          try {
            // Copy image to a temporary content to keep a safe access to it until home is saved
            imageContent = TemporaryURLContent.copyToTemporaryURLContent(
                contentManager.getContent(imageName));
          } catch (RecorderException ex) {
            // Error message displayed below
          } catch (IOException ex) {
            // Error message displayed below
          }
          if (imageContent == null) {
            if (!ignoreException) {
              EventQueue.invokeLater(new Runnable() {
                  public void run() {
                    JOptionPane.showMessageDialog(SwingUtilities.getRootPane(ImportedTextureWizardStepsPanel.this),
                        preferences.getLocalizedString(
                            ImportedTextureWizardStepsPanel.class, "imageChoiceError", imageName));
                  }
                });
            }
            return;
          }

          BufferedImage image = null;
          try {
            // Check image is less than 10 million pixels
            Dimension size = SwingTools.getImageSizeInPixels(imageContent);
            if (size.width * (long)size.height > LARGE_IMAGE_PIXEL_COUNT_THRESHOLD) {
              imageContent = readAndReduceImage(imageContent, size, preferences);
              if (imageContent == null) {
                return;
              }
            }
            image = readImage(imageContent, preferences);
          } catch (IOException ex) {
            // image is null
          }

          final BufferedImage readImage = image;
          final Content       readContent = imageContent;
          // Update components in dispatch thread
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                if (readImage != null) {
                  controller.setImage(readContent);
                  setImageChangeTexts(preferences);
                  imageChoiceErrorLabel.setVisible(false);
                  // Initialize attributes with default values
                  controller.setName(contentManager.getPresentationName(imageName,
                      ContentManager.ContentType.IMAGE));
                  // Use user category as default category and create it if it doesn't exist
                  TexturesCategory userCategory = new TexturesCategory(preferences.getLocalizedString(
                      ImportedTextureWizardStepsPanel.class, "userCategory"));
                  for (TexturesCategory category : preferences.getTexturesCatalog().getCategories()) {
                    if (category.equals(userCategory)) {
                      userCategory = category;
                      break;
                    }
                  }
                  controller.setCategory(userCategory);
                  controller.setCreator(null);
                  float defaultWidth = 20;
                  LengthUnit lengthUnit = preferences.getLengthUnit();
                  if (!lengthUnit.isMetric()) {
                    defaultWidth = LengthUnit.inchToCentimeter(8);
                  }
                  controller.setWidth(defaultWidth);
                  controller.setHeight(defaultWidth / readImage.getWidth() * readImage.getHeight());
                } else if (isShowing()) {
                  controller.setImage(null);
                  setImageChoiceTexts(preferences);
                  JOptionPane.showMessageDialog(ImportedTextureWizardStepsPanel.this,
                      preferences.getLocalizedString(
                          ImportedTextureWizardStepsPanel.class, "imageChoiceFormatError"));
                }
              }
            });
        }
      });
  }

  /**
   * Informs the user that the image size is larger and returns a reduced size image if he confirms
   * that the size should be reduced.
   * Caution : this method must be thread safe because it's called from image loader executor.
   */
  private Content readAndReduceImage(Content imageContent,
                                     final Dimension imageSize,
                                     final UserPreferences preferences) throws IOException {
    try {
      float factor;
      float ratio = (float)imageSize.width / imageSize.height;
      if (ratio < .5f || ratio > 2.) {
        factor = (float)Math.sqrt((float)LARGE_IMAGE_MAX_PIXEL_COUNT / (imageSize.width * (long)imageSize.height));
      } else if (ratio < 1f) {
        factor = (float)IMAGE_PREFERRED_MAX_SIZE / imageSize.height;
      } else {
        factor = (float)IMAGE_PREFERRED_MAX_SIZE / imageSize.width;
      }
      final int reducedWidth = Math.round(imageSize.width * factor);
      final int reducedHeight = Math.round(imageSize.height * factor);
      final AtomicInteger result = new AtomicInteger(JOptionPane.CANCEL_OPTION);
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
            String title = preferences.getLocalizedString(ImportedTextureWizardStepsPanel.class, "reduceImageSize.title");
            String confirmMessage = preferences.getLocalizedString(ImportedTextureWizardStepsPanel.class,
                "reduceImageSize.message", imageSize.width, imageSize.height, reducedWidth, reducedHeight);
            String reduceSize = preferences.getLocalizedString(ImportedTextureWizardStepsPanel.class, "reduceImageSize.reduceSize");
            String keepUnchanged = preferences.getLocalizedString(ImportedTextureWizardStepsPanel.class, "reduceImageSize.keepUnchanged");
            String cancel = preferences.getLocalizedString(ImportedTextureWizardStepsPanel.class, "reduceImageSize.cancel");
            result.set(SwingTools.showOptionDialog(SwingUtilities.getRootPane(ImportedTextureWizardStepsPanel.this),
                  confirmMessage, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                  new Object [] {reduceSize, keepUnchanged, cancel}, keepUnchanged));
          }
        });
      if (result.get() == JOptionPane.CANCEL_OPTION) {
        return null;
      } else if (result.get() == JOptionPane.YES_OPTION) {
        updatePreviewComponentsWithWaitImage(preferences);

        InputStream contentStream = imageContent.openStream();
        BufferedImage image = ImageIO.read(contentStream);
        contentStream.close();
        if (image != null) {
          BufferedImage reducedImage = new BufferedImage(reducedWidth, reducedHeight, image.getType());
          Graphics2D g2D = (Graphics2D)reducedImage.getGraphics();
          g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
          g2D.drawImage(image, AffineTransform.getScaleInstance(factor, factor), null);
          g2D.dispose();

          File file = OperatingSystem.createTemporaryFile("texture", ".tmp");
          ImageIO.write(reducedImage, image.getTransparency() == BufferedImage.OPAQUE ? "JPEG" : "PNG", file);
          return new TemporaryURLContent(file.toURI().toURL());
        }
      }
      return imageContent;
    } catch (InterruptedException ex) {
      return imageContent;
    } catch (InvocationTargetException ex) {
      ex.printStackTrace();
      return imageContent;
    } catch (IOException ex) {
      updatePreviewComponentsImage(null);
      throw ex;
    }
  }

  /**
   * Reads image from <code>imageContent</code>.
   * Caution : this method must be thread safe because it's called from image loader executor.
   */
  private BufferedImage readImage(Content imageContent,
                                  UserPreferences preferences) throws IOException {
    try {
      updatePreviewComponentsWithWaitImage(preferences);

      // Read the image content
      InputStream contentStream = imageContent.openStream();
      BufferedImage image = ImageIO.read(contentStream);
      contentStream.close();

      if (image != null) {
        updatePreviewComponentsImage(image);
        return image;
      } else {
        throw new IOException();
      }
    } catch (IOException ex) {
      updatePreviewComponentsImage(null);
      throw ex;
    }
  }

  private void updatePreviewComponentsWithWaitImage(UserPreferences preferences) throws IOException {
    // Display a waiting image while loading
    if (waitImage == null) {
      waitImage = ImageIO.read(ImportedTextureWizardStepsPanel.class.getResource(
          preferences.getLocalizedString(ImportedTextureWizardStepsPanel.class, "waitIcon")));
    }
    updatePreviewComponentsImage(waitImage);
  }

  /**
   * Updates the <code>image</code> displayed by preview components.
   */
  private void updatePreviewComponentsImage(final BufferedImage image) {
    if (EventQueue.isDispatchThread()) {
      this.imageChoicePreviewComponent.setImage(image);
      this.attributesPreviewComponent.setImage(image);
    } else {
      EventQueue.invokeLater(new Runnable() {
          public void run() {
            updatePreviewComponentsImage(image);
          }
        });
    }
  }

  /**
   * Sets the texts of label and button of image choice panel with
   * change texts.
   */
  private void setImageChangeTexts(UserPreferences preferences) {
    this.imageChoiceOrChangeLabel.setText(preferences.getLocalizedString(
        ImportedTextureWizardStepsPanel.class, "imageChangeLabel.text"));
    this.imageChoiceOrChangeButton.setText(SwingTools.getLocalizedLabelText(preferences,
        ImportedTextureWizardStepsPanel.class, "imageChangeButton.text"));
    if (!OperatingSystem.isMacOSX()) {
      this.imageChoiceOrChangeButton.setMnemonic(
          KeyStroke.getKeyStroke(preferences.getLocalizedString(
              ImportedTextureWizardStepsPanel.class, "imageChangeButton.mnemonic")).getKeyCode());
    }
  }

  /**
   * Sets the texts of label and button of image choice panel with
   * choice texts.
   */
  private void setImageChoiceTexts(UserPreferences preferences) {
    this.imageChoiceOrChangeLabel.setText(preferences.getLocalizedString(
        ImportedTextureWizardStepsPanel.class, "imageChoiceLabel.text"));
    this.imageChoiceOrChangeButton.setText(SwingTools.getLocalizedLabelText(preferences,
        ImportedTextureWizardStepsPanel.class, "imageChoiceButton.text"));
    if (!OperatingSystem.isMacOSX()) {
      this.imageChoiceOrChangeButton.setMnemonic(
          KeyStroke.getKeyStroke(preferences.getLocalizedString(
              ImportedTextureWizardStepsPanel.class, "imageChoiceButton.mnemonic")).getKeyCode());
    }
  }

  /**
   * Returns an image name chosen for a content chooser dialog.
   */
  private String showImageChoiceDialog(UserPreferences preferences,
                                       ContentManager contentManager) {
    return contentManager.showOpenDialog(this,
        preferences.getLocalizedString(ImportedTextureWizardStepsPanel.class, "imageChoiceDialog.title"),
        ContentManager.ContentType.IMAGE);
  }

  /**
   * Updates the image shown in attributes panel.
   */
  private void updateAttributesPreviewImage() {
    BufferedImage attributesPreviewImage = this.attributesPreviewComponent.getImage();
    if (attributesPreviewImage == null
        || attributesPreviewImage == this.imageChoicePreviewComponent.getImage()) {
      attributesPreviewImage = new BufferedImage(IMAGE_PREFERRED_SIZE, IMAGE_PREFERRED_SIZE, BufferedImage.TYPE_INT_RGB);
      this.attributesPreviewComponent.setImage(attributesPreviewImage);
    }
    // Fill image with a white background
    Graphics2D g2D = (Graphics2D)attributesPreviewImage.getGraphics();
    g2D.setPaint(Color.WHITE);
    g2D.fillRect(0, 0, IMAGE_PREFERRED_SIZE, IMAGE_PREFERRED_SIZE);
    BufferedImage textureImage = this.imageChoicePreviewComponent.getImage();
    if (textureImage != null) {
      // Draw the texture image as if it will be shown on a 250 x 250 cm wall
      g2D.setPaint(new TexturePaint(textureImage,
          new Rectangle2D.Float(0, 0,
              this.controller.getWidth() / 250 * IMAGE_PREFERRED_SIZE,
              this.controller.getHeight() / 250 * IMAGE_PREFERRED_SIZE)));
      g2D.fillRect(0, 0, IMAGE_PREFERRED_SIZE, IMAGE_PREFERRED_SIZE);
    }
    g2D.dispose();
    this.attributesPreviewComponent.repaint();
  }
}
