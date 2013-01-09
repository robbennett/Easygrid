Easygrid
=======================

This plugin provides a convenient and agile way of defining Data Grids.
It also provides a powerful selection widget ( a direct replacement for drop-boxes )

It handles all the middleware for ajax grids so that the developer can focus on the actual business logic.

Installation
-----------------------------

    grails install-plugin easygrid

    For minimum functionality you need: jquery-ui and export plugins.
    For google visualization you also need: google-visualization
    For the default security implementation you need spring-security.


Overview
----------------------------

The issues that Easygrid tackles are:

* big learning curve for each ajax Grid framework
* once integrated into a grails project the logic for each ajax Grid resides in multiple places ( Controller, gsp ). Usually, in the controller, there's a different method for each aspect ( search, export, security, etc)
* a lot of concerns are addressed programmatically, instead of declaratively (like search, formats )
* duplicated code (javascript, gsp, controllers). Each project has to create individual mechanisms to address it.

* combo-boxes are suitable only when the dataset is small. Easygrid proposes a custom widget based on the same mechanism of defining grids, where for selecting an element you open a grid (with pagination & filtering) in a dialog and select the desired element


[Online demo ](http://199.231.186.169:8080/easygrid)

Easygrid solves these problems by proposing a solution based on declarations & conventions.


### Features:

- custom builder - for defining the grid
- easy to mock and test: able to generate a Grid from a domain class without any custom configuration
- agile: reloads & regenerates the grid when source code is changed
- convenient default values for grid & column properties
- DRY: predefined column types - ( sets of properties )
- define the column formatters in one place
- customizable html/javascript grid templates
- built-in support for exporting to XLS ( using the exporter plugin )

- Jquery-ui widget and custom tag for a powerful selection widget featuring a jquery autocomplete textbox and a selection dialog built with Easygrid ( with filtering, sorting,etc)


Concepts
--------------------

Basically, the entire grid is defined in the Controller using the provided custom builder.

For each grid you can to configure the following aspects:

- datasource
- grid implementation
- columns:
    - name
    - value ( could be a property of the datasource row, or a closure )
    - formatting
    - Optional specific grid implementation properties ( that will be available in the renderer)
    - filterClosure - in case the grid supports per column filtering - this closure will act as a filter ( will depend on the underlying datasource )
- security
- global formatting of values
- export
- custom attributes

The plugin provides a clean separation between the model and the view ( datasource & rendering )
Currently there are implementations for 3 datasources ( _GORM_ , _LIST_ - represents a list of objects stored in the session (or anywhere), _CUSTOM_ - fully configurable)
and 4 grid implementations ( _JqGrid_, _GoogleVisualization_, _Datatables_, _static_ (the one generated by scaffolding) )



Usage
-----------------------------

All grids will be defined in controllers - which must be annotated with @Easygrid.

In each annotated controller you need to define a static closure called "grids" where you define the grids which will be made available by this controller (an AST transformation adds custom grid methods to the controller for each defined grid ).

The plugin provides a custom Builder for making the configuration more straight forward.

Ex:  grid to display Authors with 5 columns: id, name, nation , age and birthdate

    static grids = {
        'authorGrid' {
            datasourceType 'gorm'
            domainClass Author
            gridImpl 'jqgrid'
            roles 'ROLE_USER'
            columns {
                id {
                    type 'id'
                }
                name {
                    jqgrid {
                        editable true
                    }
                    export {
                        width 100
                    }
                }
                nation
                age {
                    value { row ->
                        use(TimeCategory) {
                            new Date().year - row.birthDate.time.year
                        }
                    }
                    jqgrid {
                        width 110
                        search false
                    }
                }
                birthDate{
                    jqgrid {
                        width 110
                    }
                }
            }
        }
    }

From this simple example, you can see how we can define all aspects of the grid here.
( datasource, rendering, security, export properties, filtering )



#### Grid Implementations:

*	**jqgrid**
        - implements [jqgrid](http://www.trirand.com/blog/)

*	**visualization**
        - implements [google visualization datatable](https://developers.google.com/chart/interactive/docs/reference#DataTable)

*	**datatables**
        - implements [datatables](http://datatables.net)

*	**classic**
        - implements the classic grails grid ( the static one generated by scaffolding )

 To create your own implementation you need to:

    1) create a service
    2) create a renderer
    3) declare the service & renderer in Config

On installation of the plugin , the renderer templates for the default implementations are copied to /grails-app/views/templates/easygrid from the plugin, to encourage the developer to customize them.


#### Grid Datasource Types:
*	**gorm**

        - the datasource is a Gorm domain class.
        - _domainClass_ - the domain ( mandatory)
        - _initialCriteria_  - a filter that will be applied all the time

    For fast mock-up , grids with this type can be defined without columns, in which case these will be generated at runtime from the domain properties (dynamic scaffolding).

    A filter closure is defined per column and will have to be a closure which will be used by a GORM CriteriaBuilder


*	**list**

        - used when you have a list of custom objects (for ex. stored in the session ) that must be displayed in the grid

        - _context_ - the developer must declare the context where to find the list ( defaults to session )

        - _attributeName_ - and the attribute name (in the specified context)

        - pagination will be handled by the framework


*	**custom**

        - when the list to be displayed is dynamic ( generated by a closure )

        - _dataProvider_  - closure that returns the actual data ( must implement pagination )

        - _dataCount_ - closure that returns the number of items



If you want to create your own datasource (skip this as a beginner):

1) Create a service - which must implement the following methods:
      - generateDynamicColumns() - optional - if you want to be able to generate columns
      - verifyGridConstraints(gridConfig)  - verify if the config is setup properly for this datasource impl
      - list(Map listParams = [:], filters = null) - (     * returns the list of rows
                                                           * by default will return all elements
                                                           * @param listParams - ( like  rowOffset maxRows sort order)
                                                           * @param filters - array of filters )
      - countRows(filters = null)

      - in case you want to support inline editing you need to define 3 closures ( updateRow , saveRow ,delRow)
2) declare this service in Config.groovy , under easygrid.dataSourceImplementations




#### Columns section [see] (https://github.com/tudor-malene/Easygrid/blob/master/src/groovy/org/grails/plugin/easygrid/ColumnConfig.groovy)

The _name_ of each column will be the actual name of the closure. Beside the actual column name, from the _name_ property other properties can be inferred, like:
    - the label ( the column header ) can be automatically generated ( see below)
    - in case there is no property or value setting ( see below ), _name_ will be used as the column property ( see below)
    - also you can access the columns using this _name_ ( in case you want to override some properties in the taglib - see below)
    - _name_ is also used as the name of the http parameter when filtering or sorting on a column

- Column Label:
The _label_ can be defined , but in case it's missing it will be composed automatically using the 'labelFormat' template - defined in Config.groovy. ( see comments in Config.groovy)

- Column Value:
For each column you have to define the value that will be displayed in the cell.
There's 2 options for this:
In case the type of the grid is "gorm" or "list", and you just want do display a plain property you can use "property".

Otherwise you need to use the "value" closure, whose first parameter will be the actual row, and it will return whatever you need.

(There is a possibility to define the "property" columns more compact by using the actual property as the name of the column )

- Javascript settings:
Another important section of each column is the javascript implementation section.
All the properties defined here will be available in the render template to be used in whatever way.

#### filtering:

- _enableFilter_ - if this columns has filtering enabled
- _filterFieldType_ - one of the types defined per datasource - used to generate implicit filterClosures. In case of _gorm_ the type can be inferred

- _filterClosure_:
When the user filters the grid content, these closures will be applied to the dataset.
In case of grid _type_ = _gorm_, the search closure is actually a GORM CriteriaBuilder which will be passed to the list method of the domain.

#### Export
Easygrid also comes integrated with the export plugin.
Each column has an optional export section, where you can set additional properties like width, etc.

From the example you can also notice the _type_ property of a column.
Types are defined in Config.groovy, and represent a collection of properties that will be applied to this column, to avoid duplicate settings.

Default values:
- all columns have default values ( defined in Config)- which are overriden.


#### Formatters:
 - defined globally (Config.groovy) based on the type(class) of the value ( because - usually , applications, have global settings for displaying data . Ex: date format, Bigdecimal - no of decimals, etc.)
 - formatters can also be defined per column

The format to apply to a value is chosen  this way:

1) _formatClosure_ - provided at the column level
2) _formatName_    - a format defined in the _formats_ section of the Config file
3) the type is matched to one of the types from the _formats_ section of each datasource
4) the value as it is


####  Security:

If you define the property _securityProvider_ : then it will automatically guard all calls to the grid

Easygrid comes by default with a spring security implementation.
Using this default implementation you can specify which _roles_ are allowed to view or inline edit the grid


#### Other grid properties:

You can customize every aspect of the grid - because everything is a property and can be overriden. You can also add any property to any section of the configuration, and access it from the customizable template ( or from a custom service, for advanced use cases )


Selection widget
------------------------------

The Selection widget is meant to replace drop down boxes ( select ) on forms where users have to select something from a medium or large dataset.
It is composed from a jquery autocomplete textbox ( which has a closure attached on the server side) and from a selection dialog whith a full grid (with filtering & sorting), where the user can find what he's looking for, in case he can't find it using the fast autocomplete option.
It can also by constrained by other elements from the same page or by statical values. ( for ex: in the demo , if you only want to select from british authors)

You can use any grid as a selection widget by configuring an "autocomplete" section ( currently works only with JqGrid & Gorm )

[Online demo ](http://199.231.186.169:8080/easygrid/book/create)

Like this:

    autocomplete {
        idProp 'id'                             // the id property
        labelValue { val, params ->             // the label can be a property or a closure ( more advanced use cases )
            "${val.name} (${val.nationality})"
        }
        textBoxFilterClosure { filter ->        // the closure called when a user inputs a text in the autocomplete input
            ilike('name', "%${filter.paramValue}%")
        }
        constraintsFilterClosure { filter ->    // the closure that will handle constraints defined in the taglib ( see example)
            if (filter.params.nationality) {
                eq('nationality', filter.params.nationality)
            }
        }
    }

* _idProp_       - the name of the property of the id of the selected element (optionKey - in the replaced select tag)
* _labelProp_    - each widget will display a label once an item has been selected - which could be a property of the object
* _labelValue_    - or a custom value (the equivalent of "optionValue" in a select tag)
* _textBoxFilterClosure_   - this closure is similar to _filterClosure_ - and is called by the jquery autocomplete widget to filter
* _constraintsFilterClosure_ - in case additional constraints are defined this closure is applied on top of all other closures to restrict the dataset
* _maxRows_ - the maximum rows to be shown in the jquery autocomplete widget


Taglib:
-------------

Easygrid provies the following tags:

*  ``` <grid:grid name="grid_name">  ``` will render the taglib ( see doc )

*  ``` <grid:exportButton name="grid_name">  ``` the export button ( see doc )

*  ``` <grid:selection >  ```  renders a powerful replacement for the standard combo-box see the taglib document    ( see doc and Example)


Testing:
---------------

- in each annotated controller, for each grid defined in "grids" , the plugin injects multiple methods:
                def ${gridName}Html ()
                def ${gridName}Rows ()
                def ${gridName}Export ()
                def ${gridName}InlineEdit ()
                def ${gridName}AutocompleteResult ()
                def ${gridName}SelectionLabel ()
- these can be tested in integration tests


FAQ:
------------------

### Why is the default configuration so large?    ###
A: It is so large because the plugin is highly configurable, and designed to minimize code duplication.

### I need to customize the grid template. What are the properties of the gridConfig variable from the various templates? ###
A: Check out [GridConfig](https://github.com/tudor-malene/Easygrid/blob/master/src/groovy/org/grails/plugin/easygrid/GridConfig.groovy)

### What is the deal with the Filter parameter of the filterClosures?  ###
A: Check out [Filter](https://github.com/tudor-malene/Easygrid/blob/master/src/groovy/org/grails/plugin/easygrid/Filter.groovy)

### Why does the filterClosure of the _list_ implementation have 2 parameters? ###
A: Because on this implementation you also get the current row so that you can apply the filter on it, as opposed to the _gorm_ implementation where the filter closure is a criteria.

### Isn't it bad practice to put view stuff in the controller?  ###
A: You don't have to put view stuff in the controller. You are encouraged to define column types and group view stuff in the config, and just reference that.

### I need to pass other view attributes to the ajax grid.  ###
A: No problem, everything is extensible, just put it in the builder, and you can access it in the template. If it is a implementation attribute, you are encouraged to put it in the implementation section.

### I don't use spring security, can I remove the default implementation?  ###
A: Yes you can. If there is no securityProvider defined, then no security restrictions are in place.

### Is it possible to reference the same grid in multimple gsp pages, but with slight differences?   ###
A: Yes, you can override the defined grid properties from the taglib. Check out the taglib section.

### I don't like the default export.    ###
A: No problem, you can replace the export service with your own.

### Are the grid configs thread safe?    ###
A: Yes.

### The labelFormat property is weird.  ###
A: The labelFormat is transformed into a SimpleTemplateEngine instance and populated at runtime with the gridConfig and the prefix.

### Are there any security holes I should be aware of?  ###
A: All public methods are guarded by the security provider you defined either in the config or in the grid.

### What is the difference between Easygrid and the JqGrid plugin?   ###
A: The JqGrid plugin is a wrapper over JqGrid. It provides the resources and a nice taglib. But you still have to code yourself all the server side logic.
  Easygrid goes a step further and allows you to define a grid and it will render it for you.

### I use the jqgrid plugin. How difficult is it to switch to easygrid?   ###
A: If you already use jqgrid, then you probably have the grid logic split between the controller and the view.
   If you have inline editing enabled, then you probably have at least 2 methods in the controller.
   Basically, you need to strip the grid to the minimum properties ( the columns and additional properties) , translate that intro the easygrid builder and just use the simple easygrid taglib.
   If, after converting a couple of grids, you realize there's common patterns, you are encouraged to set default values and define column types, to minimize code duplication.
   After the work is done, you will realize the grid code is down to 10%.

### I have one grid with very different view requirements from the rest. What should I do.      ###
A: You can create a gsp template just for it and set it in the builder.

### The value formatting is complicated.       ###
A: It's designed to be flexible, to be able to be used in multiple contexts.

### Can I just replace a select box with a selection widget?    ###
A:  Yes, but you will need to also specify the controller & gridName

### I want to customize the selection widget.   ###
A: Just create a new autocomplete renderer template and use the selection jquery ui widget

### I need more information on how to.. ?   ###
### I have a suggestion.  ###
A: You can raise a ticket or drop me an email : tudor.malene at gmail.com, or use the mailing list





Version History
------------------------

### 1.1.0
    - upgraded to grails 2.2.0
    - upgraded jqgrid & visualization javascript libraries
    - added support for default ( implicit ) filter Closures
    - added support for 'where queries' when defining initial criterias
    - added default values for the autocomplete section
    - the selection widget is now customizable
    - improved documentation


#### 1.0.0
    - changed the column definition ( instead of the label, the column will be defined by the name)
    - resolved some name inconsistencies ( @Easygrid instead of @EasyGrid, etc )
    - grids are customizable from the taglib
    - columns are accessible by index and by name
    - rezolved some taglib inconsistencies
    - replaced log4j with sl4j


#### 0.9.9
    - first version


Upgrading to 1.1.0
-----------------
 - on install, templates are copied to the /templates/easygrid folder ( & the default configuration was updated too )
 - filter closures now have 1 parameter which is a [Filter class](https://github.com/tudor-malene/Easygrid/blob/master/src/groovy/org/grails/plugin/easygrid/Filter.groovy)
 - the labelFormat has a slightly different format (for compatibility reasons with groovy 2.0 - see the comments )
 - 'domain' datasource has been replaced with 'gorm' - for consistency
 - _maxRows_ - has been added to the autocomplete settings
 - a _filters_ section has been added to each datasource implementation , with predefined closures for different column types


Upgrading to 1.0.0
-----------------

- change the annotation to @Easygrid
- change the columns ( the column name/property in the head now instead of label)
- replace datatable to dataTables
- overwrite or merge the renderers

- In Config.groovy
    - the labelFormat is now a plain string:  labelFormat = '${labelPrefix}.${column.name}.label'
    - replace EasyGridExportService with EasygridExportService
    - replace DatatableGridService with DataTablesGridService and datatableGridRenderer with dataTablesGridRenderer

- configure the label format for grids
- in the taglib - replace id with name



Next features:
-----------------------

- other grid implementations ( like TreeGrid , Yui datatable)
- users should be able to select the columns they want to see ( & store these settings)
- other export formats and more export options
- selection widget extended to other grids
- selection widget with multiple selection
- scaffolding templates



License
-------

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.