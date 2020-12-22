$ ->
  my.initAjax()

  apiUrl =
    valPost: '/createPatient'

  vm = ko.mapping.fromJS
    firstName: ''
    lastName: ''
    passportSn: ''
    email: ''
    phone: ''
    login: ''
    password: ''
    getPatientsList: []

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
    else if !vm.login()
      toastr.error("please enter your login:")
      return no
    else if !vm.password()
      toastr.error("please enter your password:")
      return no
    else
      patient =
        firstName: vm.firstName()
        lastName: vm.lastName()
        passportSn: vm.passportSn()
        email: vm.email()
        phone: vm.phone()
        login: vm.login()
        password: vm.password()
      $.post(apiUrl.valPost, JSON.stringify(patient))
      .fail handleError
      .done (response) ->
        toastr.success(response)


  ko.applyBindings {vm}