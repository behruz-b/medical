$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    statsUrl: '/stats/get-stats'

  vm = ko.mapping.fromJS
    language: Glob.language
    stats: []

  handleError = (error) ->
    $.unblockUI()
    if error.status is 401
      my.logout()
    else if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  vm.convertStringToDate = (stringDate) ->
    moment(stringDate).format('DD/MM/YYYY HH:MM')

  getStats = ->
    $.get(apiUrl.statsUrl)
    .fail handleError
    .done (response) ->
      console.log(response)
      vm.stats(response)
  getStats()

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else 3
    vm.labels[fieldName][index]

  vm.labels =
    statistics: [
      "Statistics"
      "Статистика"
      "Statistikalar"
    ]
    createdAt: [
      "Created at"
      "Время регистрации"
      "Ro'yhatdan o'tgan vaqti"
    ]
    companyCode: [
      "Company code"
      "Код компании"
      "Kompaniya kodi"
    ]
    action: [
      "Action"
      "Действие"
      "Amal"
    ]
    login: [
      "Login"
      "Логин"
      "Login"
    ]
    ipAddress: [
      "IP Address"
      "IP адрес"
      "IP manzil"
    ]
    userAgent: [
      "User Agent"
      "User Agent"
      "User Agent"
    ]

  ko.applyBindings {vm}