ko.bindingHandlers.masked =
  init: (element, valueAccessor, allBindingsAccessor) ->
    mask = allBindingsAccessor().mask or {}
    $(element).mask mask
    ko.utils.registerEventHandler element, 'blur keypress keyup', ->
      observable = valueAccessor()
      observable $(element).val()
  update: (element, valueAccessor) ->
    try
      $el = $(element)[0]
      start = $el.selectionStart
      end = $el.selectionEnd
    catch e
      console.log(e)
    value = ko.utils.unwrapObservable(valueAccessor())
    $(element).val value
    try
      $el.setSelectionRange(start, end)
    catch e
