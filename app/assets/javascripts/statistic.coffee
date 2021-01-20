$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    statsUrl: '/getStats'

  vm = ko.mapping.fromJS
    language: Glob.language
    stats: []

  handleError = (error) ->
    if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  getStats = ->
    $.get(apiUrl.statsUrl)
    .fail handleError
    .done (response) ->
      console.log(response)
      vm.stats(response)
  getStats()


  ko.applyBindings {vm}