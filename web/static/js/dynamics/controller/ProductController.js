/**
 * Product controller.
 * 
 * @constructor
 * @base BacklogController
 * @param {Integer} id Product id.
 * @param {DOMElement} element DOM parent node for the story table. 
 */
var ProductController = function ProductController(options) {
  this.id = options.id;
  this.productDetailsElement = options.productDetailsElement;
  this.projectListElement = options.projectListElement;
  this.iterationListElement = options.iterationListElement;
  this.assigmentListElement = options.assigmentListElement;
  this.hourEntryListElement = options.hourEntryListElement;
  this.backlogsElement = options.backlogsElement;
  this.storyTreeElement = options.storyTreeElement;
  this.tabs = options.tabs;
  
  var me = this;
  this.textFilterElement = options.textFilterElement;
  this.textFilter = new SearchByTextWidget($('#searchByText'), { searchCallback: function() { me.filter(); } });
  
  this.init();
  this.initializeProductDetailsConfig();
  this.initializeProjectListConfig();
  this.initAssigneeConfiguration();
  
  this.changeTabIfFragmentFound();
  
  this.paint();
  window.pageController.setMainController(this);
};
ProductController.prototype = new BacklogController();

ProductController.prototype.filter = function() {
  var activeTab = this.tabs.tabs("option","selected");
  if (activeTab === 0) {
    this.storyTreeController.filter(this.getTextFilter(), this.getStateFilters());
  }
  else if (activeTab === 1) {
    this.projectListView.filter();
  }
};

ProductController.prototype.getStateFilters = function() {
  return this.storyTreeController.storyFilters.statesToKeep;
};

ProductController.prototype.getTextFilter = function() {
  return this.textFilter.getValue();
};

ProductController.prototype.paintStoryTree = function() {
  if(!this.storyTreeController) {
   this.storyTreeController =  new StoryTreeController(this.id, "product", this.storyTreeElement, {}, this);
   
  } 
  this.storyTreeController.refresh();
};

ProductController.prototype.projectRowControllerFactory = function(view, model) {
  var projectController = new ProjectRowController(model, view, this);
  this.addChildController("project", projectController);
  return projectController;
};

ProductController.prototype.paintProductDetails = function() {
  this.productDetailsView = new DynamicVerticalTable(this, this.model, this.productDetailConfig,
      this.productDetailsElement);
  this.productDetailsView.render();
};

ProductController.prototype.paintBacklogWidgets = function() {
  this.backlogsElement.load('ajax/productBacklogView.action?productId=' + this.id, jQuery.proxy(function() {
    this.backlogsElement.find('.widgetList > li').each(function() {
      var staticWidget = $(this).hasClass('staticWidget');
      $(this).aefWidget({
        ajaxWidget: false,
        initialReload: false,
        realWidget: !staticWidget
      });
    });
  }, this));
};

ProductController.prototype.paintProjectList = function() {
  var me = this;
  if(!me.projectListView) {
    me.projectFilters = ["ONGOING","FUTURE"];
    $('<span>Show <input type="checkbox" name="ONGOING" checked="checked"/>' + 
        'Ongoing <input type="checkbox" name="FUTURE" checked="checked"/> ' +
        'Future <input type="checkbox" name="PAST" /> ' +
        'Past projects </span>').appendTo(this.projectListElement).find("input")
        .click(function(event) {
          var el = $(this);
          var type = el.attr("name");
          if(el.is(":checked") && jQuery.inArray(me.projectFilters, type) === -1) {
            me.projectFilters.push(type);
          } else {
            ArrayUtils.remove(me.projectFilters, type);
          }
          me.projectListView.filter();
    });
    this.projectListView = new DynamicTable(this, this.model, this.projectListConfig,
        this.projectListElement);
    this.projectListView.setFilter(function(projectObj) {
      if(projectObj.getScheduleStatus() && jQuery.inArray(projectObj.getScheduleStatus(), me.projectFilters) === -1) {
        return false;
      }
      var text = me.getTextFilter();
      if(text.length > 0 && projectObj.getName().indexOf(text) === -1) {
        return false;
      }
      return true;
    });
  }
  this.model.reloadProjects();
};

/**
 * Initialize and render the page.
 */
ProductController.prototype.paint = function() {
  var me = this;
  var tab = this.tabs.tabs("option","selected");
  ModelFactory.initializeFor(ModelFactory.initializeForTypes.product,
      this.id, function(model) {
        me.model = model;
        me.paintProductDetails();
        if(tab === 1) {
          me.paintProjectList();
        }
        if (tab === 2) {
          me.paintBacklogWidgets();
        }
      });
  if(tab === 0) {
    this.paintStoryTree();
  }
  this.tabs.bind("tabsselect",function(event, ui){
    me.textFilter.clear();
    if(me.storyTreeController) {
      me.storyTreeController.resetFilter();
    }
    if(ui.index === 0) {
      me.paintStoryTree();
    } else if(ui.index === 1) {
      me.paintProjectList();
    } else if(ui.index === 2) {
      me.paintBacklogWidgets();
    }
  });
};

ProductController.prototype.changeTabIfFragmentFound = function() {
  var hash = window.location.hash;
  if (hash.match(/fi\.hut\.soberit\.agilefant\.model\.Story_(\d+)/)) {
    this.tabs.tabs('select',0);
  }
};

/**
 * Populate a new, editable project row to the table. 
 */
ProductController.prototype.createProject = function() {
  var mockModel = ModelFactory.createObject(ModelFactory.typeToClassName.project);
  mockModel.setParent(this.model);
  mockModel.setStartDate(new Date().getTime());
  mockModel.setEndDate(new Date().getTime());
  var controller = new ProjectRowController(mockModel, null, this);
  var row = this.projectListView.createRow(controller, mockModel, "top");
  controller.view = row;
  row.autoCreateCells([ProjectRowController.columnIndices.actions]);
  row.render();
  controller.openRowEdit();
};



/**
 * Initialize product details configuration.
 */
ProductController.prototype.initializeProductDetailsConfig = function() {
  var config = new DynamicTableConfiguration( {
    leftWidth: '20%',
    rightWidth: '79%',
    closeRowCallback: null
  });
  config.addColumnConfiguration(0, {
    title : "Name",
    get : ProductModel.prototype.getName,
    editable : true,
    edit : {
      editor : "Text",
      required: true,
      set: ProductModel.prototype.setName
    }
  });
  config.addColumnConfiguration(1, {
    title: "Reference ID",
    get: BacklogModel.prototype.getId,
    decorator: DynamicsDecorators.quickReference
  });
  config.addColumnConfiguration(2, {
    title : "Description",
    get : ProductModel.prototype.getDescription,
    editable : true,
    decorator: DynamicsDecorators.emptyDescriptionDecorator,
    edit : {
      editor : "Wysiwyg",
      set: ProductModel.prototype.setDescription
    }
  });
  this.productDetailConfig = config;
};

ProductController.prototype.removeProduct = function() {
  var me = this;
  var dialog = new LazyLoadedFormDialog();
  dialog.init({
    title: "Delete product",
    url: "ajax/deleteProductForm.action",
    disableClose: true,
    data: {
      ProductId: me.model.getId()
    },
    okCallback: function(extraData) {
      var confirmation = extraData.confirmationString;
      if (confirmation && confirmation.toLowerCase() == 'yes') {
        confirmation = extraData.confirmationString;
        if (confirmation && confirmation.toLowerCase() == 'yes') {
          window.location.href = "deleteProduct.action?confirmationString=yes&productId=" + me.model.getId();
        }
      }
    },
    closeCallback: function() {
      dialog.close();
    }
  });
};

/**
 * Initialize project list
 */
ProductController.prototype.initializeProjectListConfig = function() {
  var config = new DynamicTableConfiguration( {
    rowControllerFactory : ProductController.prototype.projectRowControllerFactory,
    dataSource : ProductModel.prototype.getProjects,
    caption : "Projects",
    dataType: "project",
    captionConfig: {
      cssClasses: ""
    },
    cssClass: "product-project-table",
    validators: [ BacklogModel.Validators.dateValidator ]
  });

  config.addCaptionItem( {
    name : "createProject",
    text : "Create project",
    cssClass : "create",
    callback : ProductController.prototype.createProject
  });


  config.addColumnConfiguration(ProjectRowController.columnIndices.link, {
    minWidth : 25,
    autoScale : true,
    title : "ID",
    headerTooltip : 'Project id [link to page]',
    cssClass: "backlog-id-link",
    get : ProjectModel.prototype.getId,
    decorator: DynamicsDecorators.backlogNameLinkDecorator,
    sortCallback: DynamicsComparators.valueComparatorFactory(ProjectModel.prototype.getId)
  });
  config.addColumnConfiguration(ProjectRowController.columnIndices.name, {
    minWidth : 150,
    autoScale : true,
    title : "Name",
    headerTooltip : 'Project name',
    get : ProjectModel.prototype.getName,
    sortCallback: DynamicsComparators.valueComparatorFactory(ProjectModel.prototype.getName),
    editable : true,
    edit : {
      editor : "Text",
      set : ProjectModel.prototype.setName,
      required: true
    }
  });
  config.addColumnConfiguration(ProjectRowController.columnIndices.assignees, {
    minWidth : 130,
    autoScale : true,
    title : "Assignees",
    headerTooltip : 'Project assignees',
    get : BacklogModel.prototype.getAssignees,
    decorator: DynamicsDecorators.responsiblesDecorator,
    editable : true,
    openOnRowEdit: false,
    edit : {
      editor : "Autocomplete",
      dataType: "usersAndTeams",
      set : BacklogModel.prototype.setAssignees
    }
  });
  config.addColumnConfiguration(ProjectRowController.columnIndices.status, {
    minWidth : 25,
    autoScale : true,
    title : "Status",
    headerTooltip : 'Project status',
    get : ProjectModel.prototype.getStatus,
    decorator: DynamicsDecorators.projectStatusDecorator,
    editable : true,
    edit : {
      editor : "Selection",
      set : ProjectModel.prototype.setStatus,
      items : DynamicsDecorators.projectStates
    }
  });
  config.addColumnConfiguration(ProjectRowController.columnIndices.startDate, {
    minWidth : 80,
    autoScale : true,
    title : "Start date",
    headerTooltip : 'Start date',
    get : ProjectModel.prototype.getStartDate,
    sortCallback: DynamicsComparators.valueComparatorFactory(ProjectModel.prototype.getStartDate),
    decorator: DynamicsDecorators.dateTimeDecorator,
    defaultSortColumn: true,
    editable : true,
    edit : {
      editor : "Date",
      decorator: DynamicsDecorators.dateTimeDecorator,
      set : ProjectModel.prototype.setStartDate,
      withTime: true,
      required: true
    }
  });
  config.addColumnConfiguration(ProjectRowController.columnIndices.endDate, {
    minWidth : 80,
    autoScale : true,
    title : "End date",
    headerTooltip : 'End date',
    get : ProjectModel.prototype.getEndDate,
    sortCallback: DynamicsComparators.valueComparatorFactory(ProjectModel.prototype.getEndDate),
    decorator: DynamicsDecorators.dateTimeDecorator,
    editable : true,
    edit : {
      editor : "Date",
      decorator: DynamicsDecorators.dateTimeDecorator,
      set : ProjectModel.prototype.setEndDate,
      withTime: true,
      required: true
    }
  });
  config.addColumnConfiguration(ProjectRowController.columnIndices.actions, {
    minWidth : 26,
    autoScale : true,
    title : "Edit",
    subViewFactory : ProjectRowController.prototype.projectActionFactory
  });
  config.addColumnConfiguration(ProjectRowController.columnIndices.description, {
    fullWidth : true,
    visible : false,
    get : ProjectModel.prototype.getDescription,
    cssClass : 'productstory-data',
    editable : true,
    edit : {
      editor : "Wysiwyg",
      set : ProjectModel.prototype.setDescription
    }
  });
  config.addColumnConfiguration(ProjectRowController.columnIndices.buttons, {
    fullWidth : true,
    visible : false,
    cssClass : 'productstory-data',
    subViewFactory : DynamicsButtons.commonButtonFactory
  });

  this.projectListConfig = config;
};