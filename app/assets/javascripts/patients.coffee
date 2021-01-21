$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    patientsUrl: '/get-patients'

  vm = ko.mapping.fromJS
    language: Glob.language
    patients: []

  handleError = (error) ->
    if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  vm.getPatients = ->
    $.get(apiUrl.patientsUrl)
    .fail handleError
    .done (response) ->
      vm.patients(response)

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else 3
    vm.labels[fieldName][index]

  vm.labels =
    firstName: [
      "First name"
      "Имя"
      "Ism"
    ]
    lastName: [
      "Last name"
      "Фамилия"
      "Familiya"
    ]
    email: [
      "Email"
      "Эл. адрес"
      "Email"
    ]
    phoneNumber: [
      "Phone number"
      "Телефонный номер"
      "Telefon raqami"
    ]
    passportSerialNumber: [
      "Passport serial number"
      "Серийный номер паспорта"
      "Pasport seriya raqami"
    ]
    yourID: [
      "You are registered on ID:"
      "Вы зарегистрированы по ID:"
      "Sizning ID:"
    ]

  ko.applyBindings {vm}