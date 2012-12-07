Easygrid
=======================

This plugin provides a convenient and agile way of defining Data Grids.
It also provides a powerful selection widget ( a direct replacement for drop-boxes )


Installation
-----------------------------

    grails install-plugin easygrid

    For minimum functionality you need: jquery-ui and export plugins.
    For google visualization you also need: google-visualization
    For the default security implementation you need spring-security.


Overview
----------------------------

The issues that Easygrid tackles are:

- big learning curve for each javascript ( Grid ) framework
- the logic for each grid resides in multiple places ( Controller, gsp ). Usually, in the controller, there's a different method for each aspect ( search, export, security, etc)
- a lot of concerns are addressed programmatically, instead of declaratively (like search, formats )
- duplicated code (javascript, gsp, controllers). Each project has to create individual mechanisms to address it.
- combo-boxes are suitable only when the selected dataset is small. Easygrid proposes a custom widget based on the same mechanism of defining grids, where for selecting an element you open a grid (with pagination & filtering) in a dialog and select the desired element


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

Basically, all aspects of a grid are defined in the Controller using the easygrid custom builder .
For each grid you need to configure the following aspects.

####Mandatory:

- datasource
- grid implementation
- columns:
    - name
    - value ( could be a property of the datasource row, or a closure )
    - formatting
    - Optional specific grid implementation properties ( that will be available in the renderer)
    - filterClosure - i case the grid support per column filtering - this closure will act as a filter ( will depend on the underlying datasource )

####Optional:

- security
- global formatting of values
- export


The plugin provides a clean separation between the model and the view ( datasource & rendering )
Currently there are implementation for 3 datasources ( GORM , list - represents a list from the session, custom - fully configurable)
and 4 grid implementations ( JqGrid, Google visualization, Datatables, static table (the one generated by scaffolding) )



Usage
-----------------------------

All grids will be defined in controllers - which must be annotated with @Easygrid.

In each annotated controller you need to define a static closure called "grids" where you define the grids which will be made available by this controller

The plugin provides a custom Builder for making the configuration more straight forward

Ex:  grid to display Authors with 5 columns: id, name, nation , age and birthdate

    static grids = {
        'authorGrid' {
            datasourceType 'domain'
            domainClass Author
            gridImpl 'jqgrid'
            roles 'ROLE_USER'
            columns {
                id {
                    type 'id'
                }
                name {
                    property 'name'
                    filterClosure {params ->
                        ilike('name', "%${params.name}%")
                    }
                    jqgrid {
                        editable true
                    }
                    export {
                        width 100
                    }
                }
                nation {
                    property 'nation'
                    filterClosure {params ->
                        ilike('nation', "%${params.nation}%")
                    }
                }
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
                    filterClosure {params ->
                        eq('birthDate', params.birthDate)
                    }
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

*	**classic**
        - implements the classic grails grid ( the static one generated by scaffolding )

*	**jqgrid**
        - implements [jqgrid](http://www.trirand.com/blog/)

*	**visualization**
        - implements [google visualization datatable](https://developers.google.com/chart/interactive/docs/reference#DataTable)

*	**datatables**
        - implements [datatables](http://datatables.net)

 To create your own implementation you need to:

    1) create a service
    2) create a renderer
    3) declare the service & renderer in Config

On installation of the plugin , the renderer templates for the default implementations are copied to /grails-app/views/templates.


#### Grid Datasource Types:
*	**domain**

        - the datasource is a Gorm domain class.
        - domainClass - the domain ( mandatory)
        - initialCriteria  - a filter that will be applied all the time

    For fast mock-up , grids with this type can be defined without columns, in which case these will be generated at runtime from the domain properties.

    The search closure is defined per column and will have to be a closure which will be used by a  GORM CriteriaBuilder


*	**list**

        - used when you have a clear list of objects (stored in the session) that must be displayed in the grid

        - attributeName - is the attribute name (in the specified context)

        - context - the context where to find the list ( defaults to session )

        - pagination will be handled by the framework


*	**custom**

        - when the list to be displayed is dynamic

        - dataProvider  - closure that returns the actual data ( must implement pagination )

         - dataCount - closure that returns the number of items


If you want to create your own datasource:

1) Create a service - which must have the following methods:
      - generateDynamicColumns() - optional - if you want to be able to generate columns
      - verifyGridConstraints(gridConfig)  - verify if the config is setup properly for this datasource impl
      - list(Map listParams = [:], filters = null) - (     * returns the list of rows
                                                           * by default will return all elements
                                                           * @param listParams - ( like  rowOffset maxRows sort order)
                                                           * @param filters - array of filters )
      - countRows(filters = null)

      - in case you want to support inline edit you need to define 3 closures ( updateRow , saveRow ,delRow)
2) declare this service in Config.groovy , under easygrid.dataSourceImplementations




#### Columns section
The _name_ of each column will be the actual name of the closure. The _name_ has multiple implications:
    - the label can be automatically formed ( see below)
    - in case there is no property or value setting ( see below ), the _name_ will be used as the column property ( see below)
    - also you can access the columns using this _name_ ( in case you want to override some properties in the taglib - see below)
    - the _name_ is also used as the name of the parameters when searching or sorting a column

- Column Label:
The label can be defined , but in case it's missing it will be composed automatically using the 'labelFormat' template - defined in Config.groovy.

- Column Value:
For each column you have to define the value that will be displayed.
Currently there's 2 options. In case the type of the grid is "domain" or "list", and you just want do display a plain property you can use "property".

Otherwise you can use the "value" closure, whose first parameter will be the actual row, and it will return whatever you need.

(There is a possibility to define the "property" columns more compact by using the actual property as the name of the column ( the label will be generated using labelPrefix)

- Javascript settings:
Another important section of each column is the javascript implementation section.
All the properties defined here will be available in the render template to be used in whatever way.

- _filterClosure_:
When the user filters the grid content, these closures will be applied to the dataset.
In case of grid _type_ = _domain_, the search closure is actually a GORM CriteriaBuilder which will be passed to the list method of the domain.

#### Export
Easygrid also comes integrated with the export plugin.
Each column has an optional export section, where you can set additional properties like width, etc.

From the example you can also notice the "type" property of a column.
Types are defined in Config.groovy, and represent a collection of properties that will be applied to this column, to avoid duplicate settings.

Default values:
- all columns have default values ( defined in Config)- which are overriden.


#### Formatters:
 - defined globally (Config.groovy) based on the type(class) of the value ( because - usually , applications, have global settings for displaying data . Ex: date format, Bigdecimal - no of decimals, etc.)
 - formatters can also be defined per column

The format to apply to a value is chosen  this way:

1) _formatClosure_ - provided at the column level
2) _formatName_    - of a previously defined format
3) depending of the type of the value
4) the value ast it is


####  Security:

If you define the property securityProvider: then it will automatically guard all calls to the grid

Easygrid comes by default with a spring security implementation.
Using this default implementation you can specify which _roles_ are allowed to view or inline edit the grid


#### Other grid properties:

You can customize every aspect of the grid - because everything is a property and can be overriden


Selection widget
------------------------------

The Selection widget is meant to replace drop down boxes on forms where users have to select something from a medium or large table.
It is composed from a autocomplete textbox ( which has a closure attached on the server side) and from a selection dialog whith a full grid (with filtering & sorting), where the user can find what he's looking for.
It can also by constrained by other elements from the same page or by statical values. ( for ex: in the demo , if you only want to select from british authors)

You can use any grid as a selection widget by configuring an "autocomplete" section ( currently works only with JqGrid & Gorm )

[Online demo ](http://localhost:8080/easygrid_example/book/create)

Like this:

    autocomplete {
        idProp 'id'                             // the id property
        labelValue { val, params ->             //  or a closure
            "${val.name} (${val.nationality})"
        }
        textBoxFilterClosure { params ->        // the closure called when a user inputs a text in the autocomplete input
            ilike('name', "%${params.term}%")
        }
        constraintsFilterClosure { params ->    // the closure that will handle constraints defined in the taglib ( see example)
            if (params.nationality) {
                eq('nationality', params.nationality)
            }
        }
    }

- idProp       - represents the id
- labelProp    - each widget will display a label once an item has been selected ( which could be a property of the object
- labelValue    - or a custom value
- textBoxFilterClosure   - this closure is similar to _filterClosure_ - and is called by the jquery autocomplete widget
- constraintsFilterClosure - in case additional constraints are defined this closure is applied on top of all other closures to restrict the dataset



Tablib:
-------------

Easygrid provies the following tags:

- <grid:grid name="grid_name"> - will render the taglib ( see doc )

- <grid:exportButton name="grid_name"> - the export button ( see doc )

- <grid:selection > - renders a powerful replacement for the standard combo-box see the taglib document    ( see doc and Example)


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



Version History
------------------------

#### 1.0.0
    - changed the column definition ( instead of the label, the column will be defined by the name)
    - resolved some name inconsistencies ( @Easygrid instead of @EasyGrid, etc )
    - grids are customizable from the taglib
    - columns are accessible by index and by name
    - rezolved some taglib inconsistencies
    - replaced log4j with sl4j


#### 0.9.9
    - first version



Upgrading to 1.0
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