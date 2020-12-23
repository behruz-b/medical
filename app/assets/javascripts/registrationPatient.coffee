$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    valPost: '/createPatient'

  vm = ko.mapping.fromJS
    firstName: ''
    lastName: ''
    passportSn: ''
    email: ''
    phone: ''
    getPatientsList: []
    language: Glob.language

  handleError = (error) ->
    if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  vm.onSubmitValidation = ->
    if !vm.firstName()
      toastr.error("Please enter your firstName:")
      return no
    else if !vm.lastName()
      toastr.error("please enter your lastName:")
      return no
    else if !vm.passportSn()
      toastr.error("please enter your passportSN:")
      return no
    else if !vm.phone()
      toastr.error("please enter your phone:")
      return no
    else
      patient =
        firstName: vm.firstName()
        lastName: vm.lastName()
        passportSn: vm.passportSn()
        email: vm.email()
        phone: vm.phone()
      $.post(apiUrl.valPost, JSON.stringify(patient))
      .fail handleError
      .done (response) ->
        toastr.success(response)

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else 3
    vm.labels[fieldName][index]

  vm.labels =
    welcome: [
      "Welcome to Smart Medical!"
      "Добро пожаловать в Smart Medical!"
      "Smart Medical-ga xush kelibsiz!"
    ]

  ko.applyBindings {vm}