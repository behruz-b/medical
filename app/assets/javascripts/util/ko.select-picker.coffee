ko.bindingHandlers.selectPicker =
  init: (element, valueAccessor, allBindingsAccessor) ->
    if $(element).is('select')
      if ko.isObservable(valueAccessor())
        if $(element).prop('multiple') and $.isArray(ko.utils.unwrapObservable(valueAccessor()))
          # in the case of a multiple select where the valueAccessor() is an observableArray, call the default Knockout selectedOptions binding
          ko.bindingHandlers.selectedOptions.init element, valueAccessor, allBindingsAccessor
        else
          # regular select and observable so call the default value binding
          ko.bindingHandlers.value.init element, valueAccessor, allBindingsAccessor
      $(element).addClass('selectpicker').selectpicker()
    return
  update: (element, valueAccessor, allBindingsAccessor) ->
    if $(element).is('select')
      selectPickerOptions = allBindingsAccessor().selectPickerOptions
      if typeof selectPickerOptions != 'undefined' and selectPickerOptions != null
        options = selectPickerOptions.optionsArray
#        optionsText = selectPickerOptions.optionsText
#        optionsValue = selectPickerOptions.optionsValue
#        optionsCaption = selectPickerOptions.optionsCaption
        isDisabled = selectPickerOptions.disabledCondition or false
        resetOnDisabled = selectPickerOptions.resetOnDisabled or false
        if ko.utils.unwrapObservable(options).length > 0
          # call the default Knockout options binding
          ko.bindingHandlers.options.update element, options, allBindingsAccessor
        if isDisabled and resetOnDisabled
          # the dropdown is disabled and we need to reset it to its first option
          $(element).selectpicker 'val', $(element).children('option:first').val()
        $(element).prop 'disabled', isDisabled
      if ko.isObservable(valueAccessor())
        if $(element).prop('multiple') and $.isArray(ko.utils.unwrapObservable(valueAccessor()))
          # in the case of a multiple select where the valueAccessor() is an observableArray, call the default Knockout selectedOptions binding
          ko.bindingHandlers.selectedOptions.update element, valueAccessor
        else
          # call the default Knockout value binding
          ko.bindingHandlers.value.update element, valueAccessor
      $(element).selectpicker 'refresh'
    return

