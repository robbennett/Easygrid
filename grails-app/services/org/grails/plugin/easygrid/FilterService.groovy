package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j

/**
 * responsible for the creation of filters
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class FilterService {
    static transactional = false

    /**
     *
     * @param gridConfig
     * @param filterableConfig
     * @return
     */
    Filter createFilterFromColumn(GridConfig gridConfig, FilterableConfig filterableConfig,
                                  def operator, String value) {
        def f = new Filter()
        f.filterable = filterableConfig

        f.paramName = filterableConfig.name
        f.paramValue = value
        //todo - return validation
        f.value = GridUtils.convertValueUsingBinding(f.paramValue, filterableConfig.dataType)

        f.operator = operator ?: filterableConfig.defaultFilterOperator

        if (filterableConfig.filterClosure) {
            f.searchFilter = filterableConfig.filterClosure.curry(f)
        }
        f
    }


    Filter createGlobalFilter(Closure c) {
        def f = new Filter()
        f.global = true
        f.searchFilter = c.curry(EasygridContextHolder.params)
        f
    }

    Filters createGlobalFilters(Closure c) {
        new Filters(filters: [createGlobalFilter(c)])
    }

}