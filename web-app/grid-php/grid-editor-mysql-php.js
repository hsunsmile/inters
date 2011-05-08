/**
 * Editor Grid
 * An example of an editor grid that interacts with server side php/MySQL
 * November 25, 2007
 * Michael LeComte
 * This module is based on several examples and snippets from others
 */

/**
 * This file works with
 * Ext JS Library 2.0 RC 1
 * Copyright(c) 2006-2007, Ext JS, LLC.
 * licensing@extjs.com
 * http://extjs.com/license
 */

///////////////////////////////////////////////////////////////////////////////

// This line should be included in all files in order to reference a local
// blank image (by default BLANK_IMAGE_URL = http://www.extjs.com)
Ext.BLANK_IMAGE_URL = '../../resources/images/default/s.gif';


/* Create a namespace Object
 * By specifying our own namespace we encapsulate all variables and methods in 
 * one global object in order to avoid any conflicts or changes when working 
 * with other javascript files. This pattern scales well as the project becomes
 * more complex and as its API grows. It stays out of the global namespace, 
 * provides publicly addressable API methods, and supports protected or 
 * “private” data and methods along the way.
 */
Ext.namespace('myNameSpace'); //define namespace with some 'name'
/* So we have assigned an empty object 'myNameSpace' as a member of the Ext
 * object (Note: this doesn’t overwrite 'myNameSpace' if it already exists).
 */

   
// Now that we have defined our namespace we can begin adding members to the
// Ext.myNameSpace application using the Module Pattern
myNameSpace.myModule = function(){
/** We have created a property 'myModule' of the namespace object
 *  'myNameSpace' which is divided into Private and Public Areas.  The sequence
 *  of execution actually jumps to the end of this file, where this property
 *  gets initialized and then the public initialization method is called, which
 *  starts the flow leading back into the Private Area.
 */ 

	///////////////////////////////////////////////////////////////////////////
    //-----------------------------------------------------------------------//
    //-----------------------------Private Area------------------------------//
    //-----------------------------------------------------------------------//
	///////////////////////////////////////////////////////////////////////////
    //  The Private Area is only accessible via the Public Area
    //  You can not access the Private Area directly from outside of the module 
    
	///////////////////////////////////////////////////////////////////////////
    //---------------------------Private Variables---------------------------//
	///////////////////////////////////////////////////////////////////////////
    //  Warning: Do not access DOM in this area. This area will be executed
	//           before the page has finished loading, so some elements
	//           probably do not exist yet.

    // Example of Private Variable (just an example; not needed for the grid
    var myPrivateVar = 	"I can be accessed only from within " + 
						"Ext.myNameSpace.myModule.";

    // Private variables specific to this module
    var colModel;    //definition of the columns
	var ds;			 //primary data store
    var dsIndustry;  //secondary data store (dropdowns)
    var filters;	 //definition of filter plugin
	var grid;        //the grid component (object)
    var myReader;    //reader
    var myRecordObj; //data record object
	var pagingPlugin;//definition of paging plugin     
    var primaryKey='companyID'; //primary key is used several times throughout
	var renderPctChange;
	var win;		 //pop up window

    //this is just some sample data used when right clicking a row in
	//the grid and selecting 'Properties' 
	var data = {
        name: 'Jack Slocum',
        company: 'Ext JS, LLC',
        address: '4 Red Bulls Drive',
        city: 'Cleveland',
        state: 'Ohio',
        zip: '44102',
        kids: [{
            name: 'Sara Grace',
            age:3
        },{
            name: 'Zachary',
            age:2
        },{
            name: 'John James',
            age:0
        }]
    };


	///////////////////////////////////////////////////////////////////////////
    //----------------------------Private Methods----------------------------//
	///////////////////////////////////////////////////////////////////////////
    //  Caution: Do not access DOM in this area unless you are calling these
	//           methods after the page has finished loading (onReady)

    // Example of Private Method (just an example; not needed for the grid
	var myPrivateMethod = function () {
        Ext.log("I can be accessed only from within Ext.myNameSpace.myModule");
    }

	///////////////////////////////////////////////////////////////////////////

   /**
    * 1.0 Setup the Data Source (setupDataSource)
    * 1.1 Create Data Record
    * 1.2 Define Reader
    * 1.3 Create Data Store(s)
    */
    var setupDataSource = function(){

        /**
         * 1.1 Create Data Record
         * This creates a constructor for a specific record layout defining the
         * fields that make up an individual row of data.
         * The Reader is what links the Data Object (server side source or other
         * static variable) with the DataStore.
         * The Data Record tells the Reader how to associate (or map) the data
         * from the Data Object with the Data Store.
         * You can create the data record inline during creation of the reader,
         * but creating the object separately here offers us the ability to add
         * records dynamically.  As an example of this, see the addRecord() 
         * function below.
         * To create a data record, we pass in an array of field definition
         * objects specifying how to reference the data in the Data Object (the
         * source of your data, ie. a database or static variable 'myData')
         */
        myRecordObj = Ext.data.Record.create([
		/**
		 * Note the order of the fields defined here does not matter. The order
		 * specified here does not relate to the order of the fields in your
		 * source, nor the order displayed in the grid. You also do not have to
		 * include every field that is in your data source here either.  So, if
		 * your database table has 10 columns, you can specify 2 here if you
		 * want 
	     * name:    The 'name' by which the field is referenced within
	     *          the Record. Whatever is specified here should match
	     *          the columnModel "dataIndex" property
	     * mapping: Mapping is only required if the 'reference' does
	     *          not equal the mapping, where 'reference' is the key
	     *          in the source data file, eg. db field name, xml
	     *          tag name, etc.
	     * sortDir: initial sort direction
	     * sortType: defines explicitly how to perform the sort, see
	     *           Ext.data.SortTypes
	     * type:    How the data should be displayed
         *          When type='date' make sure to specify a dateFormat that
         *          matches what is coming from the data source (see 'Class
         *          Date' in API)
		 * Example from XML data:
		 * - <Item>
		 *       <ASIN>0446355453</ASIN> 
		 *       <DetailPageURL>http://www.amazon.com/something</DetailPageURL>
		 *     - <ItemAttributes>
		 *           <Author>Sidney Sheldon</Author> 
		 *           <Manufacturer>Warner Books</Manufacturer>
		 *           <ProductGroup>Book</ProductGroup>
		 *           <Title>Master of the Game</Title>
		 *       </ItemAttributes>
		 *   </Item>
		 *
		 *	{name:'Author', mapping: 'ItemAttributes > Author'},//example of nested reference 
		 * 	{name:'Title'}, 
		 *	{name:'Manufacturer'}, 
		 *	{name:'ProductGroup'}
		 */
            {name: primaryKey},//this corresponds to 'companyID', I assigned a
                               //variable to this value since it is used
							   //multiple times throughout this code (see the
							   //Private Variable definitions)
            {name: 'company', mapping: 'company', sortDir: 'ASC', sortType: 'asUCString'},
			/**
			 * I've noticed the sorting is questionable when using paging.  For
			 * example, if the record spans multiple pages, the company "3M" doesn't
			 * show up first.  If all records are loaded into a single page view
			 * then "3M" shows up at the top of the ASC sorted column.
			 */
            {name: 'price', type: 'float'},
            {name: 'tax', type: 'float'},  
            {name: 'change', type: 'float'}, //will use mapping='change'
            {name: 'pctChange', type: 'float'},
            {name: 'lastChange', type: 'date', dateFormat: 'Y-m-d H:i:s'},
            {name: 'industryID'},
            {name: 'risk'},
            {name: 'stars'},
            {name: 'check'} //again, the order shown here does NOT matter
        ]);

     
        /**
         * 1.2. Define Reader
         * The reader extracts the field's value from the data object creating 
         * an Array of Ext.data.Record objects.
         * The Data Object can be in several different base formats, so we
         * need to use the proper reader that knows how to deal with the data
         * format. So we specify the reader based on the format of the data
         * returned (Xml, Json, Array)
         * To configure the reader, we need to specify where the reader should
         * look in the response for the data, aka the 'root'.  
         * We can also optionally tell the reader where to look for the total
         * number of records the Data Object has in case we don't pass all of
         * the data in one go (pagination, etc.).
         * 
		 * Available reader types:
		 * myReader = new Ext.data.ArrayReader(//create array from static array
		 * myReader = new Ext.data.JsonReader( //create array from JSON response
	     * myReader = new Ext.data.XmlReader(  //create array from XML response
         */
        myReader = new Ext.data.JsonReader( //creates array from JSON response
		/**
		 * The 1st parameer for the reader constructor is to specify the
		 * reader's config options:
         */
		{
			/**
			 * Specify the name of the property that is the container for an
			 * array of row objects
			 * Use record if using XmlReader or
			 * Use root   if using JsonReader
             */	
            root: 'results', //delimiter tag for each record (row of data)
			/**
			 * Optionally specify the element/property that contains the total
			 * dataset size.  This is the name of the property from which to
			 * retrieve the total number of records in the dataset.  This is
			 * useful if the whole dataset is not passed in one go, but is
			 * being paged from the remote server.
			 * Use totalRecords  if using XmlReader or
			 * Use totalProperty if using JsonReader
             */	
            totalProperty: 'total',//element containing total dataset size (opt 
      		//groupField: 'size',
			//sortInfo: {field: 'name', direction: 'ASC'},
			//remoteSort: true,
			/**
			 * Optionally specify the property within each row object that
			 * provides an ID for the record
             */	
            id: primaryKey //used several times so a Private Variable is used
            /**
             * When using JSON, the response should come back like this:
             *   (Using Firebug, click on 'Net' tab, then click the line
             *    corresponding to the name of your serverside script, then
             *    click "Response" to view the response.  You can also get
             *    same information using the "Console" tab)
 			 *   Sample 1:
 			 *   ({"total":"29",  <----the 'totalProperty' (totalProperty: 'total')
             *     "results":[    <----the 'root'          (root: 'results')
             *         {"companyID":"1","company":"Alcoa","price":"24",
             *              "change":"0.42","pctChange":"1.47",
             *              "lastChange":"2007-10-02 00:00:00",
             *              "industryID":"5","risk":"low","stars":"5",
             *              "check":"1","tax":1.44}, ...
             *     ]
             *   })
             *  
   			 *   Sample 2:
             *   {"meta":{"code":1,"exception":[],"success":true,"message":null},
             *    "data":{"total":30,   <----the 'totalProperty' (totalProperty: 'data.total')
             *            "results":[   <----the 'root'          (root: 'data.results')
             *                {"id":"26","name":...},
             *                {"id":"42","name":...},
             *                {"id":"12","name":...}
             *            ]
             *   }}
             */
        },
			/**
			 * The 2nd parameter for the reader constructor is a record 
			 * constructor object that specifies the record definition (the
			 * recordType object containing the field mapping).  This can be
			 * specified inline, or as shown below you can pass a reference to
			 * the object
			 */
            myRecordObj//pass a reference to the object 
        );
		
		
		
        /**
         * 1.3. Create Data Store(s)
         * Set up the data Store object
         * A 'Store' is a client side cache of Ext.data.Record objects which
         * provide input data for widgets.
         * Basically you create this representation of your server side data
         * by defining the objects which specify:
         * 1. proxy - 
         * 			how to the read the raw data (which proxy type)
         *          where to read data (the url to the data source)
         *          method to read data (GET or POST request) 
         * 2. reader - takes data and breaks into columns
         */ 
        // Data Store #1
        ds = new Ext.data.GroupingStore({ //if grouping
      //ds = new Ext.data.Store({ //if not grouping
            /**
             * 1. Specify how and where to access data.
             * If data is specified within this file this argument is null,
             * otherwise there are a few options for pulling the data:
             *  a. HttpProxy      reads data from the same domain/server
             *  b. ScriptTagProxy reads data object from different domain/server
             *  c. MemoryProxy    passes data specified in constructor
             * Note: specifying a proxy is required for ScriptTagProxy, however
             * if using HttpProxy you can just specify the url property (an
             * HttpProxy is automatically created)*/
            proxy: new Ext.data.HttpProxy({
//	        	url: 'sheldon.xml', //where to retrieve data
                url: 'grid-editor-mysql-php.php', //url to data object (server side script)
                method: 'POST'
            }),   
            baseParams:{task: "readStock"},//This parameter is passed for any
            			//HTTP request, I'm using it to pass along additional
						//POST parameters to use in the server side script.
						//In the server side script I check this 'task'
						//property in a switch statement to decide which
						//method to run.
            /**
             * 2. Specify the reader
             * The Store object doesn't know what the data looks like or or
             * what format it is in, so a 'reader' is used to read the data
             * into Records. The reader object defined here will process the
             * data object and return an array of Ext.data.Record objects
             * which are cached and keyed per their id property mapping the
             * data we need
             */
            reader: myReader,
            //groupField:'industry', //added for GroupingStore, specifies initial group sort
            sortInfo:{field: 'company', direction: "ASC"}
            //remoteSort: true,//true if sort from server (false = sort from cache only)
        });//end ds

        // Data Store #2
		// This store will hold the data for the dropdown options
        dsIndustry = new Ext.data.Store({
            proxy: new Ext.data.HttpProxy({
                //where to retrieve data
                url: 'grid-editor-mysql-php.php', //url to data object (server side script)
                method: 'POST'
            }),   
            baseParams:{task: "readIndustry"},//this parameter is passed for any HTTP request
            /*2. specify the reader*/
            reader:  new Ext.data.JsonReader(
				{
					root: 'results',//name of the property that is container for an Array of row objects
					id: 'industryID'//the property within each row object that provides an ID for the record (optional)
				},
				[
					{name: 'industryID'},//name of the field in the stock table (not the industry table)
					{name: 'industryName'}
				]
            ),
            sortInfo:{field: 'industryName', direction: "ASC"}
        }
        );//end dsIndustry        

	};//end setupDataSource  





	///////////////////////////////////////////////////////////////////////////





   /**
    * 2.0 Get the Column Model (getColumnModel)
    *     We have all of this data that came from the source (possibly server
    *     side) and then we read that data into a client side cache (store).
    *     Now we have to decide what parts of that data we want to display and
    *     how we want to display it.  We may have some data we don't want
    *     displayed, or want some of it shown in a particular way. 
    * 2.1 Create Custom Renderers
    * 2.2 Create the Column Model
    * 2.3 Set up Plugins
    */
    var getColumnModel = function(){
        if(!colModel) { //only need to create columnModel if it doesn't already exist

            /**
             * 2.1. Create Custom Renderers
             * (listed alphabetically)
             */

            /** 
             * Date renderer function
             * Renders a date
             * @param {Object} val
             */
            function renderDate(value){
				//Ext.util.Format.dateRenderer('m/d/Y')
				return value ? value.dateFormat('M d, Y') : '';
            };

  			/** 
  			 * Italic Custom renderer function
  			 * takes val and renders it in italics
  			 * @param {Object} val
  			 */
            function italic(val){
                return '<i>' + val + '</i>';
            };

            /** 
             * Percent Custom renderer function
             * takes val and renders it red or green with %
             * @param {Object} val
             */
//            function renderPctChange(val){
            this.renderPctChange = function(val){
                if(val >= 0){
				//-> this = obj (row from grid, properties of id, name='pctChange', style)
                    return '<span style="color:green;">' + val + '%</span>';
                }else if(val < 0){
                    return '<span style="color:red;">' + val + '%</span>';
                }
                return val;
            };

            /** 
             * Red/Green Custom renderer function
             * takes val and renders it red if <0 otherwise renders it green 
             * @param {Object} val
             */
            function renderPosNeg(val){
                if(val >= 0){
				//-> this = obj (row from grid, properties of id, name='change', style)
                    return '<span style="color:green;">' + val + '</span>';
                }else if(val < 0){
                    return '<span style="color:red;">' + val + '</span>';
                }
                return val;
            };


            /** 
             * Risk Custom renderer function
             * Renders according to risk level
             * @param {Object} val
             */
            function renderRisk(data, cell, record, rowIndex, columnIndex, store){
				switch(data) {
					case "high":
						cell.css = "redcell";
						return "high";//display 'high' in the cell (could be
						              //we could display anything here
									  //"High","Hi","yup"...anything
					case "medium":
						return "medium";
					case "low":
						return "low";
				}
            };

            /** 
             * Star Custom renderer function
             * Renders a picture according to value
             * @param {Object} val
             */
            function renderStars(data, cell, record, rowIndex, columnIndex, store){
				switch(data) {
					case "1":	cell.css = "stars1";   return 1;//returns text over the background image
					case "2":	cell.css = "stars2";   return;//just shows the background image
					case "3":	cell.css = "stars3";   return;
					case "4":	cell.css = "stars4";   return;
					case "5":	cell.css = "stars5";   return;
				}
            };


	///////////////////////////////////////////////////////////////////////////

            
            /** 
             * 2.2 Create the Column Model
			 */

			/**
			 * Define column to pass into the Column Model by reference
			 */
			this.checkColumn = new Ext.grid.CheckColumn({
				header: "Check",
				dataIndex: 'check', 
				width: 9, 
				sortable: true
			});

			/**
			 * Load store(s) that the Column Model uses
			 * ** The 'main' data store gets loaded later **
			 * Note: The store loaded here holds the data for the dropdown options.
			 * Originally I had this store load later, but when I did that the
			 * grid would sometimes get rendered the first time apparently before 
			 * the dsIndustry store was loaded (this caused 'missing data'
			 * to get displayed as per the renderer function shown below)
			 */
			//ds.loadData is used to load data directly from a static data
			//source (for example an array in this file called 'myData')
		 	//ds.loadData(myData);
		    
			//Use ds.load to load data from dynamic data source
			dsIndustry.load(); //industry column dropdown options


            /** 
             * Set up the ColumnModel object to define the initial layout /
             * display of the grid. The order specified here defines the order
             * of the initial column display. We also define how each column in
             * the grid correlates (maps) to our DataStore with dataIndex. 
             *   "mapping"   specifies how the data object relates/maps to the
             *               DataStore (client side cache of data).
             *   "dataIndex" specifies how the DataStore   relates/maps to the
             *               ColumnModel (actual display).
             * We can extend this to provide custom or reusable ColumnModels 
             */
            colModel = new Ext.grid.ColumnModel([ //instantiate ColumnModel
		        /**
		         * Here we give comma separated definitions of the fields we
		         * want displayable (some may be initially hidden) in the grid.
		         * Note you need not display every column in your data store 
		         * here; you can include fields here and have them be hidden or
		         * you can just not include some fields in your grid whatsoever
		         * (maybe you just retrieved them to do other behind the scenes
		         * processing client side instead of server side)
		         */ 
                 {  
				    /*optionally specify the aligment (default = left)*/
					align: 'right',
					
					/*[Required] Specify the dataIndex which is the DataStore
					 * field "name" this column draws its data from */
                    dataIndex: 'companyID',
					
                    header:'ID',//header = text that appears at top of column
                    //hidden: true, //true to initially hide the column
                    
					/**
					 * We can optionally place an id on a column so we can later
					 * reference the column specifically. This might be useful
					 * for instance if we want to set a css style to highlight
					 * the column (.x-grid3-col-classCompanyID)
					 * This doesn't appear to work well with an editor grid
					 * though, as the red triangle gets covered up. As a guess, 
					 * perhaps something with the z-index could be changed so the
					 * red triangle remained on top?
					 * Another use might be to select an entire column and do
					 * something with it. Example anyone?
					 */ 
                    id:    'classCompanyID',
                    //locked: false,//no longer supported, see user extensions
                    sortable: true,//false (default) disables sorting by this column
                    width: 9       //column width
                 },{                         
                    dataIndex: 'company',
                    header:"Company",
                    id:    'classCompany',
                    locked: false,
                    sortable: true,
                    //resizable: false,//disable column resizing (can also use fixed = true)
                    width: 40,      
					
					//TextField editor - for an editable field add an editor
                    editor: new Ext.form.TextField({ 
                        //specify options
                        allowBlank: false //default is true (nothing entered)
                    })                           
                },{                         
                    align: 'right',//default is to align to the left
                    dataIndex: 'price',
                    header: "Price", 
                    sortable: true,
                    width: 12, 

	                /* (NOTE: as of Ext2.0-rc1 the GroupingStore has problems
					 * sizing the grid when columns are initially hidden)  */
                    //hidden: true,//true to initially hide the column 
                    
					/* optional rendering function to provide customized data
					 * formatting */
					renderer: Ext.util.Format.usMoney,
                    editor: new Ext.form.NumberField({
                        //specify options
                        allowBlank: false,  //default is true (nothing entered)
                        allowNegative: false, //could also use minValue
                        maxValue: 100
                    })
                },{                         
                    align: 'right',
                    dataIndex: 'tax',
                    header: "Tax", 
                    renderer: Ext.util.Format.usMoney,
                    sortable: true,
                    width: 12
                },{
                    align: 'center',
                    dataIndex: 'change',
                    header: "Change", 
					//custom renderer specified by reference:
                    renderer: renderPosNeg,
                    sortable: true, 
                    width: 12
                },{
					dataIndex: 'pctChange',
					header: "% Change",
					renderer: this.renderPctChange,
					sortable: true,
					width: 15
				},{
                    dataIndex: 'lastChange',
                    header: "Last Updated", 
                    renderer: renderDate, 
                    sortable: true, 
                    width: 20, 
                    editor: new Ext.form.DateField({ //DateField editor
                        //specify options
						
						//allow to leave blank? default is true (allow blanks)
                        allowBlank: false, 
						
						//defaults to 'm/d/y', if there is a renderer 
                        //specified it will render whatever this form
                        //returns according to the renderer
                        //format: 'd/m/y', 
						
						//specify a minimum value such that anything prior to
						//this date is greyed out/unclicklable.  The validator
						//prevents typing a new date violating criteria 
                        minValue: '10/15/07',
                        disabledDays: [0, 3, 6],
                        disabledDaysText: 'Closed on this day'
                    })
                },{
					dataIndex: 'industryID',
					header: "Industry",
					sortable: true, 
					width: 23, 
					
					//create a dropdown based on server side data (from db)
					editor: new Ext.form.ComboBox({ 
						//if we enable typeAhead it will be querying database
						//so we may not want typeahead consuming resources
						typeAhead: false, 
						triggerAction: 'all',
						
						//By enabling lazyRender this prevents the combo box
						//from rendering until requested
						lazyRender: true,//should always be true for editor

						//where to get the data for our combobox
						store: dsIndustry,
						
						//the underlying data field name to bind to this
						//ComboBox (defaults to undefined if mode = 'remote'
						//or 'text' if transforming a select)
						displayField: 'industryName',
						
						//the underlying value field name to bind to this
						//ComboBox
						valueField: 'industryID'
					}),
					renderer:  //custom rendering specified inline
							function(data) {
								record = dsIndustry.getById(data);
								if(record) {
									return record.data.industryName;
								} else {
									//return data;
									return 'missing data';
								}
							}
				},{
					dataIndex: 'risk',
					header: "Risk",
					renderer: renderRisk, 
					sortable: true, 
					width: 11, 
					
					//dropdown based on client side data (from html)
					editor: new Ext.form.ComboBox({ 
						typeAhead: true,
						triggerAction: 'all',
						
						//look for this id to transform the html option values
						//to a dropdown
						transform:'riskID',
						lazyRender:true,
						
						//css class to apply to the dropdown list element
						listClass: 'x-combo-list-small' 
					})
				},{
					align: 'center',
					dataIndex: 'stars',
					header: "Stars",
					renderer: renderStars, 
					sortable: true, 
					width: 8
				},
				
				//this column is passed in by reference (see above)
				checkColumn 
				
            ]);//end colModel
            
            // Instead of specifying sorting permission by individual columns
			// can also specify for entire grid
            //colModel.defaultSortable = false;


            /** 
             * 2.3 Set up Plugins
             */
			
			/** 
             * Filtering Plugin
			 * Author = Ambience
			 * Specify filtering options for each column 
			 * Filters specified:
			 *  - do not have to match the data record definition
			 *  - must match the column model definition
			 * You can have filters specified here that are not in the column
			 * model. Every column in the column model must have a filter
			 * specified here
			 * Examples:
			 * {dataIndex: 'price'},//can't do - must specify type also
			 * {type: 'numeric', dataIndex: 'something', value: {eq:42}},
			 * {type: 'numeric', dataIndex: 'something', value: {gt:0, lt 3.2}},
			 * {type: 'date',  dataIndex: 'something', value: {on:'1/1/2007'}}
			 * {type: 'date',  dataIndex: 'something', value: {after:'1/1/2007',
			 * 				before:'12/1/2007'}}
			 * {type: 'date',  dataIndex: 'dateAdded'},
			 * {type: 'string',  dataIndex: 'visible', active:true, 
			 * 				value:'someString'},
			 * {type: 'boolean', dataIndex: 'visible', active:true, 
			 * 				defaultValue:true}
			 * {type: 'list', dataIndex: 'size', options: ['extra small',
			 * 				'small', 'medium', 'large', 'extra large'],
			 * 				phpMode: true},
			 * 
			 * What does phpMode do?  Here are the post requests when:
			 * 
			 * phpMode = true, after 1st select
             * 
			 * filter[0][data][type]	list
			 * filter[0][data][value]	low
			 * filter[0][field]	risk
			 * limit	10
			 * start	0
			 * task	readStock
             * 
             * 
			 * phpMode = true, after 2nd select
             * 
			 * filter[0][data][type]	list
			 * filter[0][data][value]	low,medium <==comma separated
			 * filter[0][field]	risk
			 * limit	10
			 * start	0
			 * task	readStock
             * 
             * 
			 * phpMode = false, after 2nd select
             * 
			 * filter[0][data][type]	list
			 * filter[0][data][value]	medium
			 * filter[0][data][value]	low
			 * filter[0][field]	risk
			 * limit	10
			 * start	0
			 * task	readStock
             */ 
			this.filters = new Ext.ux.grid.GridFilters({
				//need to specify if want local filtering (filter the store),
				//otherwise defaults to server side filtering
				local:true,//specify true if you want to filter client side
				
				filters:[
		            {dataIndex: primaryKey,   type: 'numeric'},
		            {dataIndex: 'company', 	  type: 'string'},
		            {dataIndex: 'price', 	  type: 'numeric'},
		            {dataIndex: 'tax', 	 	  type: 'numeric'},
		            {dataIndex: 'change', 	  type: 'numeric'},
		            {dataIndex: 'pctChange',  type: 'numeric'},
		            {dataIndex: 'lastChange', type: 'date'},
		            {dataIndex: 'industryID', type: 'string'},
		            {dataIndex: 'risk', 	  type: 'list',
						active:false,//whether filter value is activated
						value:'low',//default filter value
						options: ['low','medium','high'],
						
						//if local = false or unspecified, phpMode has an effect
						phpMode: false},
		            {dataIndex: 'stars', 	  type: 'numeric'},
		            {dataIndex: 'check', 	  type: 'boolean'}
				]
			});//end filters setup


			/** 
             * Paging Plugin
             * Author = devnull / modified by Andrie
			 */
			this.pagingPlugin = new Ext.ux.Andrie.pPageSize({
				afterText: 'records at a time'
			});

        }//end if colModel
        
		return colModel;//if colModel already exists return it
		
    }//end getColumnModel





	///////////////////////////////////////////////////////////////////////////





   /**
    * 3.0 Build the Grid (buildGrid)
    * 3.1 Define Event Handlers 
    * 3.2 Create (instantiate) the Grid 
    * 3.3 Add listeners to the Grid 
    * 3.4 Render the Grid (make it lazy if you want) 
    * 3.5 Load Data Store(s)(load store after grid is rendered)
    */
    var buildGrid = function(){
	
        /**
         * 3.1. Create Handlers
         * Create functions to handle various events
         * (listed alphabetically)
         */


        /**
         * Handler for Adding a Record
         */
        function addRecord() {
            var r = new myRecordObj({
                //specify default values
				
				//will use this to trigger special handling when updating
                companyID: 0,
				
				//you can't comment out this line if you want the editor
                //to start there, as it will show the html tags 
				company: '', 
                price: 0.00,
                tax: 0.00,
                change: 0.00,
                pctChange: 0.00,
                lastChange: (new Date()).clearTime(),
				industry: '',
				risk: '',
				stars: '0'
            });
            grid.stopEditing();//stops any acitve editing

            //very similar to ds.add, with ds.insert we can specify
			//the insertion point
            ds.insert(0, r); //1st arg is index,
                             //2nd arg is Ext.data.Record[] records
            
			//start editing the specified rowIndex, colIndex
            //make sure you pick an editable location
			//otherwise it won't initiate the editor
			grid.startEditing(0, 1);
        }; // end addRecord 


        function callDelete(item) {
            Ext.MessageBox.alert('Request','You selected '+this.text);
        }; 


        function callTrade(item) {
            Ext.MessageBox.alert('Request','You selected '+this.text);
        }; 


        function callPrintPreview(item) {
            Ext.MessageBox.alert('Request','You selected '+this.text);
        }; 


        /**
         * Function for Deleting record(s)
         * @param {Object} btn
         */ 
        function deleteRecord(btn) {
            if(btn=='yes')
            {
                /* block of code if we just want to remove 1 row
                var selectedRow = grid.getSelectionModel().getSelected();//returns record object for the most recently selected
                                                                     //row that is in data store for grid
                if(selectedRow){
                    ds.remove(selectedRow);
                } //end of block to remove 1 row
                */
				
				//returns record objects for selected rows (all info for row)
                var selectedRows = grid.selModel.selections.items;
				
                //returns array of selected rows ids only
				var selectedKeys = grid.selModel.selections.keys; 

                //note we already did an if(selectedKeys) to get here

                //encode array into json
                var encoded_keys = Ext.encode(selectedKeys);
                //submit to server
                Ext.Ajax.request( //alternative to Ext.form.FormPanel? or Ext.BasicForm.submit
                    {   //specify options (note success/failure below that receives these same options)
                        waitMsg: 'Saving changes...',
                        //url where to send request (url to server side script)
                        url: 'grid-editor-mysql-php.php',
						
						//params will be available via $_POST or $_REQUEST:
                        params: { 
                            task: "delete", //pass task to do to the server script
                            companyID: encoded_keys,//the unique id(s)
                            key: primaryKey//pass to server same 'id' that the reader used
                        },
                        
						/**
						 * You can also specify a callback (instead of or in
						 * addition to success/failure) for custom handling.
						 * If you have success/failure defined, those will
						 * fire before 'callback'.  This callback will fire
						 * regardless of success or failure.*/
                        callback: function (options, success, response) {
                            if (success) { //success will be true if the request succeeded
                                Ext.MessageBox.alert('OK',response.responseText);//you won't see this alert if the next one pops up fast
                                var json = Ext.util.JSON.decode(response.responseText);

								//need to move this to an after event because
								//it will fire before the grid is re-rendered
								//(while the deleted row(s) are still there
                                Ext.MessageBox.alert('OK',json.del_count + ' record(s) deleted.');
                                
								//You could update an element on your page with
								//the result from the server
								//(e.g.<div id='total'></div>)
                                //var total = Ext.get('total');
                                //total.update(json.sum);
								
                            } else{
                                Ext.MessageBox.alert('Sorry, please try again. [Q304]',response.responseText);
                            }
                        },
                        
                        //the function to be called upon failure of the request (server script, 404, or 403 errors)
                        failure:function(response,options){
                            Ext.MessageBox.alert('Warning','Oops...');
                            //ds.rejectChanges();//undo any changes
                        },                                      
                        success:function(response,options){
                            //Ext.MessageBox.alert('Success','Yeah...');
							//commit changes and remove the red triangle which
							//indicates a 'dirty' field
                            ds.reload();
                        }                                      
                     } //end Ajax request config
                );// end Ajax request initialization
            };//end if click 'yes' on button
        }; // end deleteRecord 


		/**
         * Function for updating Tax shown in grid
         * @param {Object} oGrid_Event
         */
        function getTax(oGrid_Event) {
            
			//submit to server
            Ext.Ajax.request( //alternative to Ext.form.FormPanel? or Ext.BasicForm
                {   
                    //waitMsg: 'Saving changes...',
                    //url where to send request (url to server side script)
                    url: 'grid-editor-mysql-php.php', 
                    //method: 'POST', 
					
					//params will be available via $_POST or $_REQUEST:
                    params: { 
                        task: "calcTax", //pass task to do to the server script
                        price: oGrid_Event.value//the updated value
                    },//end params
                    //the function to be called upon failure of the request
                    failure:function(response,options){
                        Ext.MessageBox.alert('Warning','Oops...');
                        //ds.rejectChanges();//undo any changes
                    },//end failure block                                      
                    success:function(response,options){
						//Ext.MessageBox.alert('Success','Yeah...');
						var responseData = Ext.util.JSON.decode(response.responseText);//passed back from server
						
						//extract the value provided by the server
						var myTax = responseData.tax;
						//oGrid_Event.record.data.tax = myTax;//assign the tax to the record
						//oGrid_Event.record.tax= myTax;//assign the id to the record
						oGrid_Event.record.set('tax',myTax);

						//commit changes (removes the red triangle which
						//indicates a 'dirty' field)
						ds.commitChanges();
                    }//end success block                                      
                 }//end request config
            ); //end request  
        }; //end getTax 

        
        /**
         * Handler for Deleting record(s)
         */ 
        function handleDelete() {
			
			//returns array of selected rows ids only
            var selectedKeys = grid.selModel.selections.keys; 
            if(selectedKeys.length > 0)
            {
                Ext.MessageBox.confirm('Message','Do you really want to delete selection?', deleteRecord);
            }
            else
            {
                Ext.MessageBox.alert('Message','Please select at least one item to delete');
            }//end if/else block
        }; // end handleDelete 


		/**
		 * Handler to control grid editing
		 * @param {Object} oGrid_Event
		 */
		function handleEdit(editEvent) {
			//determine what column is being edited
			var gridField = editEvent.field;
			
			//start the process to update the db with cell contents
			updateDB(editEvent);
			
			//I don't want to wait for server update to update the Total Column
			if (gridField == 'price'){
				getTax(editEvent);//start the process to update the Tax Field
			}
		}

		/**
		 * onCheckColumn handler
		 * @param {Object} element
		 * @param {Object} e
		 * @param {Object} record
		 */
		function onCheckColumn(element, e, record) {
			//remember 'on' is just a shortcut for 'addListener'
			//maybe this should listen to 'dblclick' instead?
			
		  	//alert(record.get('check'));
			var myField = this.dataIndex;//the field name
			var check = record.data[this.dataIndex];//same as record.data.check (but more abstract)
			var checkStatus = check ? 'checked' : 'unchecked';
			var checkItem = record.data.company;
			var checkID = record.data.companyID;
			Ext.example.msg('Item Check', 'You {0} the "{1}" check box, ID = {2}.', checkStatus, checkItem, checkID);
			var myMsg = 'You <b>'+ checkStatus + '</b> the "<i>' + checkItem + '</i>" check box, ID = ' + '<span style="color:blue;">' + checkID + '</span>.';
			Ext.MessageBox.alert('Item Check', myMsg);
			var checkBoolean = check ? 1 : 0;

            //update the database
			Ext.Ajax.request(
                { //ajax request configuration  
                    waitMsg: 'Saving changes...',
                    url: 'grid-editor-mysql-php.php', //url to server side script
                    params: { //these will be available via $_POST or $_REQUEST:
                        task: "update", //pass task to do to the server script
                        key: primaryKey,//pass to server same 'id' that the reader used
                        keyID: checkID,//for existing records this is the unique id (we need this one to relate to the db)
                        //newRecord: isNewRecord,//pass the new Record status indicator to server for special handling
                        field: myField,//the column name
                        value: checkBoolean,//the updated value
                        originalValue: !checkBoolean//the original value (oGrid_Event.orginalValue does not work for some reason)
                    },//end params
                    failure:function(response,options){
                        Ext.MessageBox.alert('Warning','Oops...');
                    },//end failure block                                      
                    success:function(response,options){
						//Ext.MessageBox.alert('Success','Yeah...');
                        if(checkID == 0){
							var responseData = Ext.util.JSON.decode(response.responseText);//passed back from server
							var newID = responseData.newID;//extract the ID provided by the server
							record.set('newRecord','no');//reset the indicator since update succeeded
							record.set('companyID',newID);//assign the id to the record
							ds.commitChanges();//commit changes (removes the red triangle which indicates a 'dirty' field)
						} else {
							ds.commitChanges();//commit changes (removes the red triangle which indicates a 'dirty' field)
						}
                    }//end success block                                      
                 }//end ajax request config
            ); //end ajax request  
		}; // end onCheckColumn handler
		

	    function onItemCheck(item, checked){
	        Ext.example.msg('Item Check', 'You {1} the "{0}" menu item.', item.text, checked ? 'checked' : 'unchecked');
	    }





//        var tpl = new Ext.XTemplate(
        var tpl = new Ext.XTemplate(
			'<div class="quote-item">' +
			'<h2>{company}</h2>' +
			'<img src="../../examples/shared/icons/fam/user.png" alt="Logo" style="float:right;margin:4px;" />' +  
			'<label>Price:</label>${price}<br />' + 
			'<label>Change:</label>{change}<br />' + 
			'<label>% Change:</label>{pctChange}<br />' + 
			'<label>last updated:</label>{lastChange}<br />' + 
			'</div>'
        );

    var p = new Ext.Panel({
        title: 'Basic Template',
        width: 300,
        html: '<p><i>Apply the template to see results here</i></p>',
        tbar: [
			{
	            text: 'Apply Data',
	            handler: function(){
	                tpl.overwrite(p.body, data);
	                p.body.highlight('#c3daf9', {block:true});
	            }
	        },
			{
	            text: 'Do Something Else',
	            handler: function(){
	                // code here
	            }
	        }
		],
        renderTo: 'property-win'
    });

	
        /**
         * properties
         * Method to display record data in a popup tabbed window 
         * @param {Object} item
         */
		function properties(item) {
        	var sel = grid.getSelectionModel().getSelected();
            var selIndex = ds.indexOf(sel);
            var seldata=sel.data;

        	seldata.lastChange=Ext.util.Format.date(seldata.lastChange,'m/d/Y');

/*          //Scope problem trying to access the predefined custom renderer functions
        	seldata.pctChange = this.renderPctChange(seldata.pctChange);
        	seldata.change=renderPosNeg(seldata.change);
*/
            tpl.overwrite(p.body, seldata);

	        // create the window on the first click and reuse on subsequent clicks
	        if(!win){
	            win = new Ext.Window({
	                el:'property-win',
	                layout:'fit',
	                width:500,
	                height:300,
	                closeAction:'hide',
	                plain: true,
	                items: p,
	                buttons: [{
	                    text:'Submit',
	                    disabled:true
	                },{
	                    text: 'Close',
	                    handler: function(){
	                        win.hide();
	                    }
	                }]
	            });
	        }
	        win.show(this);
	        p.body.highlight('#c3daf9', {block:true});

/*
			var tabs = new Ext.TabPanel({
			var p = new Ext.Panel({
			    //autoTabs:true,
				//region: 'center',
			    margins:'3 3 3 0', 
			    activeTab: 2,//display the 3rd tab when win opens
			    defaults:{autoScroll:true},
				//deferredRender:false,
		        //border:false,
			    items:[{
			        title: 'Bogus Tab',
					//An HTML fragment, or a DomHelper specification to use as the
					//panel's body content (defaults to '').
			        html: <p>Hello </p>
			    },{
			        title: 'Closable Tab',
			        html: <p>You selected <b>{item.text}</b></p>,
			        closable:true
			    },{
			        title: 'Data Tab',
			        html: tpl.applyTemplate(seldata),//use our template for the markup
			        html: 'Something bogus here',
			        html: test,
			        closable:true
			//  }]
			, renderTo: document.body
			});
*/			
        }; // end properties 


        //callback function for the right click event 
		function onMessageContextMenu(grid, rowIndex, e) {
			e.stopEvent();
			var coords = e.getXY();
			
			//OK, we have our record, now how do we pass
            //it to the referenced handler?
			var record = grid.getStore().getAt(rowIndex);

			var messageContextMenu = new Ext.menu.Menu({
				id: 'messageContextMenu',
				items: [
				{
					text: 'Properties',
					handler: properties,
					scope: this
				},
				{
	                text: 'I like Ext',
					//when checked has a boolean value,
					//it is assumed to be a CheckItem
	                checked: true,       
	                checkHandler: onItemCheck
	            }
				]
			});
			
			//predefine a menu item
			var menuItem = new Ext.menu.Item({text: '<i>New Item</i>'});

     		//shows how to add items dynamically
            var item = messageContextMenu.add(
				'-',
				menuItem, //add item by reference
				{	
					//handler: onMessageContextItemClick(this,['open']),
					//handler: function(){
	                //	this.viewer.openTab(this.ctxRecord);
	                //},
					iconCls: 'add',
					text: '<u>Insert above</u>',
					tooltip: 'Insert a row above this line'
				},
					{text: '<b>Print</b>', menu: new Ext.menu.Menu({// <-- submenu by nested config object
						items: [
							{text: 'PDF',  handler:function(){callPrintPreview("PDF");} },
							{text: 'EXCEL',handler:function(){callPrintPreview("EXCEL ");} },
							{text: 'HTML', handler:function(){callPrintPreview("HTML") ;} },
							{text: 'WORD', handler:function(){callPrintPreview("WORD") ;} }
						]
					})},
					{text: '<b>Save Preferences</b>',handler: function(){saveUserPref(Ext.encode(colModel.config));}}
			);

			messageContextMenu.showAt([coords[0], coords[1]]);
			e.preventDefault();//to disable the standard browser context menu
		}


        /**
         * Function for Refreshing Grid
         */ 
        function refreshGrid() {
			ds.reload();//
        }; // end refresh 


        function SaveToExcel(item) {
            Ext.MessageBox.alert('Request','You selected '+this.text);
        }; 


		/**
		 * toggleDetails
		 * @param {Object} btn
		 * @param {Object} pressed
		 */
	    function toggleDetails(btn, pressed){
			/*
	        var view = grid.getView();
	        view.showPreview = pressed;
	        view.refresh();
			*/
	    }


		/**
         * Function for updating database
         * @param {Object} oGrid_Event
         */
        function updateDB(oGrid_Event) {
            
			/**
			 * Do we need to disable a new record from further editing while
			 * the first request is being made since the record may not have
			 * the new companyID in time to use to properly handle other
			 * updates of the same record? 
			 *
			 * 
			 * Dates come through as an object instead of a string or
			 * numerical value, so do a check to prep the new value for
			 * transfer to the server side script
			 */
			if (oGrid_Event.value instanceof Date)
			{   //format the value for easy insertion into MySQL
				var fieldValue = oGrid_Event.value.format('Y-m-d H:i:s');
			} else
			{
				var fieldValue = oGrid_Event.value;
			}	
					
			//submit to server
            Ext.Ajax.request( //alternative to Ext.form.FormPanel? or Ext.BasicForm
                {   //Specify options (note success/failure below that
                    //receives these same options)
                    waitMsg: 'Saving changes...',
                    //url where to send request (url to server side script)
                    url: 'grid-editor-mysql-php.php', 
					
					//If specify params default is 'POST' instead of 'GET'
                    //method: 'POST', 
					
					//params will be available server side via $_POST or $_REQUEST:
                    params: { 
                        task: "update", //pass task to do to the server script
                        key: primaryKey,//pass to server same 'id' that the reader used
                        
						//For existing records this is the unique id (we need
						//this one to relate to the db). We'll check this
						//server side to see if it is a new record                    
                        keyID: oGrid_Event.record.data.companyID,
						
						//For new records Ext creates a number here unrelated
						//to the database
					    //-bogusID: oGrid_Event.record.id,

                        field: oGrid_Event.field,//the column name
                        value: fieldValue,//the updated value
                        
						//The original value (oGrid_Event.orginalValue does
						//not work for some reason) this might(?) be a way
						//to 'undo' changes other than by cookie? When the
						//response comes back from the server can we make an
						//undo array?                         
                        originalValue: oGrid_Event.record.modified
						
                    },//end params
                    
					//the function to be called upon failure of the request
					//(404 error etc, ***NOT*** success=false)
                    failure:function(response,options){
                        Ext.MessageBox.alert('Warning','Oops...');
                        //ds.rejectChanges();//undo any changes
                    },//end failure block      
                    
					//The function to be called upon success of the request                                
                    success:function(response,options){
						//Ext.MessageBox.alert('Success','Yeah...');
                        
						
						//if this is a new record need special handling
						if(oGrid_Event.record.data.companyID == 0){
							var responseData = Ext.util.JSON.decode(response.responseText);//passed back from server
							
							//Extract the ID provided by the server
							var newID = responseData.newID;
							//oGrid_Event.record.id = newID;
							
							//Reset the indicator since update succeeded
							oGrid_Event.record.set('newRecord','no');
							
							//Assign the id to the record
							oGrid_Event.record.set('companyID',newID);
							//Note the set() calls do not trigger everything
							//since you may need to update multiple fields for
							//example. So you still need to call commitChanges()
							//to start the event flow to fire things like
							//refreshRow()
							
							//commit changes (removes the red triangle which
							//indicates a 'dirty' field)
							ds.commitChanges();

						    //var whatIsTheID = oGrid_Event.record.modified;
						
						//not a new record so just commit changes	
						} else {
							//commit changes (removes the red triangle
							//which indicates a 'dirty' field)
							ds.commitChanges();
						}
                    }//end success block                                      
                 }//end request config
            ); //end request  
        }; //end updateDB 


	///////////////////////////////////////////////////////////////////////////


        /**
         * 3.2. Create (instantiate) the Grid
         * This creates the actual GUI for the Grid.
         * We specify here where, how, and when to render the Grid.
         */

      //grid = new Ext.grid.GridPanel({ //to instantiate normal grid
        grid = new Ext.grid.EditorGridPanel({ //to instantiate editor grid
            autoExpandColumn: 'company', //which column to stretch in width to fill up the grid width and not leave blank space
            //autoSizeColumns: true,//deprecated as of Ext2.0
            clicksToEdit:2,//number of clicks to activate cell editor, default = 2        
            colModel: getColumnModel(), //gets the ColumnModel object to use (cm: is shorthand)
            //footer: true,
            //frame:true,//add a frame around the grid; defaults to no frame 
            //autoHeight:true,//autoHeight resizes the height to show all records
            height:350,//you must specify height or autoHeight
 			iconCls: 'icon-grid',//we create our own css with a class called 'icon-grid'
			id: 'myGridID',//unique id of this component (defaults to an auto-assigned id).        
            //el:'grid-company', //html element (id of the div) where the grid will be rendered
            //'renderTo' does the same as 'el', except eliminated the need to explicitly call render() 
            //renderTo: 'grid-company',//could also render it directly to document.body 
			loadMask: true,//use true to mask the grid while loading (default = false)
            plugins:[this.checkColumn, this.filters],//object or array of objects (filters = enable filtering of records)
            //plugins: filters,//example with just one plugin
		    //Enable a Selection Model.  The Selection Model defines the selection behavior,
            //(single vs. multiple select, row or cell selection, etc.)
            selModel: new Ext.grid.RowSelectionModel({singleSelect:false}),//true to limit row selection to 1 row})
            store: ds,       //the DataStore object to use (ds: is shorthand)
			stripeRows: true,//applies css classname to alternate rows, defaults to false
            title:'This is the Grid Title',
			//trackMouseOver: true,//highligts rows on mousever (default = false for editor grid)
			width:740,
			/**
			 * The config options above create the following html
			 * 
			 *   <div id="grid-company">
			 *       <div id="myGridID" class="x-panel x-grid-panel" 
			 *            style="width: 740px;">
			 *       </div>
			 *   </div>	
			 */
            //Add a bottom bar      
            bbar: new Ext.PagingToolbar({
	            plugins: [this.pagingPlugin, this.filters],
                pageSize: myNameSpace.myModule.perPage,//default is 20
                store: ds,
                displayInfo: true,//default is false (to not show displayMsg)
                displayMsg: 'Displaying topics {0} - {1} of {2}',
                emptyMsg: "No data to display",//display message when no records found
                items:[
                    '-', {
                    pressed: true,
                    enableToggle:true,
                    text: 'Show Preview',
                    cls: 'x-btn-text-icon details',
                    toggleHandler: toggleDetails  
                }]
            }),
            //Add a top bar      
            tbar: [
                {
                    text: 'Add Record',
                    tooltip: 'Click to Add a row',
                    
					//We create our own css with a class called 'add'
                    //.add is a custom class not included in
					// ext-all.css by default, so we need to define the
					// attributes of this style ourselves
					iconCls:'add', 
                    handler: addRecord //what happens when user clicks on it
                }, '-', //add a separator
                {
                    text: 'Delete Selected',
                    tooltip: 'Click to Delete selected row(s)',

                    //function to call when user clicks on button  
                    handler: handleDelete, 
                    iconCls:'remove' 
                }, '->', // next fields will be aligned to the right 
                {
                    text: 'Refresh',
                    tooltip: 'Click to Refresh the table',
                    handler: refreshGrid,
                    iconCls:'refresh'
                }
            ],
            
			//this is the key to showing the GroupingStore
            view: new Ext.grid.GroupingView({
                forceFit:true,
                //custom grouping text template to display the number of
				//items per group
                groupTextTpl: '{text} ({[values.rs.length]} {[values.rs.length > 1 ? "Items" : "Item"]})'
            })
        });//end grid


	///////////////////////////////////////////////////////////////////////////


        /**
         * 3.3 Add listeners to the Grid
         */

        /**
         * Add right click event
         * rowcontextmenu fires when a row is right clicked
         */ 
		grid.addListener('rowcontextmenu', onMessageContextMenu);


        /**
         * Add an event/listener to handle any updates to grid
         */ 
        grid.addListener('afteredit', handleEdit);//give event name, handler (can use 'on' shorthand for addListener) 

		
		//instead of adding listeners individually could have also loaded together like so:   
        /*
	    grid.addListener({  //same as saying grid.on
			'rowcontextmenu': onMessageContextMenu,
			'afteredit': handleEdit
			//note other listeners are same just without the 'on' (mousever, mouseout, click, etc.)
		});
		*/


		/**
		 * Add an event/listener to column check boxes
		 */
		this.checkColumn.on('click', onCheckColumn); 


    }//end function buildGrid




	///////////////////////////////////////////////////////////////////////////




    /**
     * 5.0 Render the Grid
     * Explicitly rendering the grid is only required if "renderTo" is not
     * specified in the configuration of the grid object above.
     * In Ext2.0, every component automatically supports "lazy" (on demand)
     * rendering. The rendering pipeline is managed automatically if you use
     * "renderTo" in the grid constructor.
     * Instead of using "renderTo" you can explicitly render the grid when you 
     * want using render(). This gives you the flexibility / power to control
     * the rendering process (lazy render).  In addition to render() you can 
     * also use the beforerender() event (see Ext.Component).
     * Rendering the grid does not necessarily mean building rows.  If there
     * is no data, the rest of the grid layout gets built (e.g. headers,
     * footers, etc.)
     */
    var renderGrid = function() {  
        grid.render('grid-company');//1st argument is the container,
        							//2nd argument is the position within
                                    //the div (end of the container is default)

        /**
         * Grid rendering effects
         * if we want to render rows depending on values in row
         */ 
		grid.getView().getRowClass = function(record, index) {
			switch (record.data.stars) {
				case '0': //this is the default when adding records
				//Right now I shade the row yellow when stars = '0' to signify
				//the row has not been saved yet.  You might want to choose
				//another option, like maybe when 'companyID' is no longer
				//zero. For now I just decided to 'watch' the last column
				//thinking that would be the last to get updated and I wanted
				//the row to stay yellow until the row was completely updated.
					return "yellowrow"
					break
				case '5':
					return "greenrow"
					break
				case '4':
					return "pinkrow"
					break
				default:
				    //something else?
			}
        };//end row rendering


    };//end function renderGrid




	///////////////////////////////////////////////////////////////////////////




    /**
     * 6.0 Load the Store  (loadStore)
     * Method created to enable loading the stores on demand
     * 
     * "Store loading is the process of gettingthe data and building it's
     *  records, it has nothing to do with rendering.  Store fires the load 
     *  event when it's done building Records (not grid rows).  The grid 
     *  listens to events from the store (e.g. 'load') to know when the data is
     *  done loading - this drives the porcess of the grid building the HTML
     *  rows." - Tim Ryan
     */
    var loadStore = function() {  
		/** 
		 * Allegedly the proper order is to load the store after the grid is
		 * rendered.  You'll see examples done both ways.
		 * I have noticed situations where it seemed to work better if the store
		 * returned before the execution gets to the grid render() line,
		 * otherwise it will still render the grid but the store will be
		 * empty and thus display an empty grid.  If you use the paging
		 * toolbar refresh that will probably show all the data because it 
		 * just refreshes the page from the cached data store*/        
		ds.load({
			params: { //this is only parameters for the FIRST page load,
			          //use baseParams above for ALL pages.
				start: 0, //pass start/limit parameters for paging
				limit: myNameSpace.myModule.perPage//
			}
		}); 
		/**Once this line executes the request gets sent to the server.
		 * Note that this is an asychronous request, this file keeps going 
		 * independent of the server. If you're stepping through firebug,
		 * it's at this point you'll see the XHR get sent out.
		 */

		/**
		 * The following line is to highlight the first row of the grid once it
		 * is loaded.  I've noticed this line is quite finicky though.  
		 * There's probably a better way to do this that is more reliable.
		 * Allegedly you can't reliably select rows until the store is loaded,
		 * however I find the next line still doesn't work reliably, even
		 * when the store is already loaded.  I think the next line should maybe
		 * be called after something else gets finished.
		 */
        grid.getSelectionModel().selectFirstRow();

	};//end loadStore





	///////////////////////////////////////////////////////////////////////////
    //-----------------------------------------------------------------------//
    //------------------------------Public Area------------------------------//
    //-----------------------------------------------------------------------//
	///////////////////////////////////////////////////////////////////////////

    return {//returns an object=myNameSpace.myModule with the following
            //properties:

	///////////////////////////////////////////////////////////////////////////
    //---------------------------Public Properties---------------------------//
	///////////////////////////////////////////////////////////////////////////
	// It is considered good practice to put the following in the public area
	// of the module:
    //     text used by module,
	//     default dimensions, styles, 
	//     customizable options, etc.

    // Example of Public Property (just an example; not needed for the grid
        myPublicProperty: "I'm accessible as myNameSpace.myModule.myPublicProperty.",
			//To reference this property use:
			// this.myPublicProperty (while inside the Public Area)
			// myNameSpace.myModule.myPublicProperty (when outside of the
			// module)
	
    // Public Properties specific to this module
        perPage: 10, //page limit

	///////////////////////////////////////////////////////////////////////////
    //-----------------------------Public Methods----------------------------//
	///////////////////////////////////////////////////////////////////////////
	// Public methods can be called from outside the module using
	//    myNameSpace.myModule.method_name_here
	// Public methods can access Private Area
        
    // Example of Public Method (just an example; not needed for the grid

		//The following method is accessible as myNameSpace.myModule.myPublicMethod
		myPublicMethod: function(){
			
			//note the use of 'this' to refer to Public Property
            var myOtherProperty = this.myPublicProperty 
            
			//note reference to Private variable does NOT use 'this'
			return myPrivateVar; 
        },
        
    // Public Methods specific to this module
		
		//Initialization Method
		init : function(){
			//This method is called by the last line below that looks like this:
            //Ext.onReady(myNameSpace.myModule.init, myNameSpace.myModule, true);
        	//So, once the document is fully loaded, that line gets executed
			//and we end up here. As a result, this is a good place to put
			//DOM dependent tasks since we know the elements are now loaded. 


			/**
			 * Set up plugin for a check column
			 * @param {Object} config
			 */
			Ext.grid.CheckColumn = function(config){
				this.addEvents({
					click: true
				});
				Ext.grid.CheckColumn.superclass.constructor.call(this);
				
				Ext.apply(this, config, {
					init : function(grid){
					    this.grid = grid;
					    this.grid.on('render', function(){
					        var view = this.grid.getView();
					        view.mainBody.on('mousedown', this.onMouseDown, this);
					    }, this);
				    },
				
				    onMouseDown : function(e, t){
						if(t.className && t.className.indexOf('x-grid3-cc-'+this.id) != -1){
							e.stopEvent();
							var index = this.grid.getView().findRowIndex(t);
							var record = this.grid.store.getAt(index);
							record.set(this.dataIndex, !record.data[this.dataIndex]);
							this.fireEvent('click', this, e, record);
						}
				    },

				    renderer : function(v, p, record){
					    //the +v type converts to a number (json returns a string
						//which always evaluates true)
						var checkState = (+v) ? '-on' : '';
						
						p.css += ' x-grid3-check-col-td'; 
					  
					  //return '<div class="x-grid3-check-col'+(v?'-on':'')+' x-grid3-cc-'+this.id+'"> </div>';
					    return '<div class="x-grid3-check-col'+ checkState +' x-grid3-cc-'+this.id+'"> </div>';
				    }
				});
				
			  if(!this.id){
			      this.id = Ext.id();
			  }
			  this.renderer = this.renderer.createDelegate(this);
			};
			
			// extend Ext.util.Observable
			Ext.extend(Ext.grid.CheckColumn, Ext.util.Observable);

			//done with plugin setup		
			/////////////////////////////////////////////////////////////////

			Ext.ux.menu.RangeMenu.prototype.icons = {
				gt: 'images/greater_than.png', 
				lt: 'images/less_than.png',
				eq: 'images/equals.png'
			};
			Ext.ux.grid.filter.StringFilter.prototype.icon = 'images/find.png';

	 		// get our Grid (remember, 'this' refers to properties and methods
			// inside the public area
			this.getMyGrid();
	
	        //Works without this, used for state awareness...
	        //Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

        },//end of init method
       
		/**
		 * getMyGrid
		 * This method goes through the steps to create our grid
		 * 1.0 Initialization
		 *     define icons
		 * 2.0 Setup the Data Source   (setupDataSource)
		 * 		define store
		 * 		specify filters
		 * 3.0 Create the Column Model (getColumnModel)
		 * select rows or do anything else you want with rows
		 * 4.0 Build the Grid  (buildGrid)
		 * 5.0 Render the Grid (renderGrid)
		 * 6.0 Load the Store  (loadStore)
		 */    
        getMyGrid: function() {  

								//Show a progress bar just for learning purposes
								//lines related to progress bar indented more to
								//separate from the 'real' code
								var pbar2 = new Ext.ProgressBar({
									text:'Ready',
									id:'pbar2',
									cls:'left-align',
									renderTo:'p2'
								});
								
								var updateMyProgress = function(stepNo, stepDescription){
									var total = 10;
									var i = stepNo/total;
									pbar2.updateProgress(stepNo/total, 'Step ' + stepNo + ' of '+total+' - '+stepDescription+' ('+Math.round(100*i)+'% completed).');
									if (stepNo == total){
										pbar2.updateText('Done.');
									}	
								};
					
								var myStep = -1;
								updateMyProgress(myStep++,'Initialization complete');
					            updateMyProgress(myStep++,'Enabling Quicktips');
			
			Ext.QuickTips.init();//Enable Quicktips

					        	pbar2.updateText('Quicktips enabled');
		        	
								updateMyProgress(myStep++,'Setup the Data Source');
			
			setupDataSource();   //Setup the Data Source
			
								updateMyProgress(myStep++,'Create the Column Model');

			//getColumnModel();//Create the Column Model
			                   //this is invoked when the grid object is created 
            
								updateMyProgress(myStep++,'Build the Grid');

			buildGrid();         //Build the Grid
			renderGrid();        //Render the Grid
			loadStore();         //Load the Store

        },
        
		//Method to check what is in our Data Store
		//You might call this method fromt he firebug console for example
        getDataSource: function() {  
            return ds;
        }

    }//end of return
	
    //------------------------------Public Area------------------------------//
    //-----------------------------------------------------------------------//
	///////////////////////////////////////////////////////////////////////////

}(); /* End of application. Note the parentheses () — this notation causes the
		anonymous function to execute immediately, dumping us right into the 
		Private Area where we step through to load all Private Variables and 
		take an inventory of all of the Private Functions, finally the return 
		in the "Public Area" is executed in similar fashion but since we are
		"returning" we now see this "Public Area" outside of the module which
		gives us the ability to execute the line below which fires the
		initialization method in the Public Area.  
        So, as soon as the anonymous function returns:
         1. we have an object containing myPublicProperty and myPublicMethod
         2. we can address that returned object as myNameSpace.myModule. */


/* Since the above code has already executed, we are able to access any Public
   Properties (Public Variables and Public Methods), including the "init"
   method immediately.
   The following execution line executes the myModule.init method after the
   document has been completely loaded. This line also sets the myModule.init
   method scope to myModule, which means you can call Public Attributes (methods
   and properties) with a preceding 'this'. */
Ext.onReady(myNameSpace.myModule.init, myNameSpace.myModule, true);