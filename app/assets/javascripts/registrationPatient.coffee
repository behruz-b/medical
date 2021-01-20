$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    registerUrl: '/patient'

  defaultPatient =
    firstName: ''
    lastName: ''
    phone: ''

  vm = ko.mapping.fromJS
    patient: defaultPatient
    customerId: ''
    language: Glob.language

  handleError = (error) ->
    if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  $thankYou = $('#thankYou')

  vm.onSubmit = ->
    toastr.clear()
    if !vm.patient.firstName()
      toastr.error("Iltimos ismingizni kiriting!")
      return no
    else if !vm.patient.lastName()
      toastr.error("Iltimos familiyangizni kiriting!")
      return no
    else if !vm.patient.phone()
      toastr.error("Iltimos telefon raqamingizni kiriting!")
    else if vm.patient.phone() and !my.isValidPhone(vm.patient.phone().replace(/[(|)|-]/g, "").trim())
      toastr.error("Iltimos telefon raqamingizni to'gri kiriting!")
      return no
    else
      patient =
        firstName: vm.patient.firstName()
        lastName: vm.patient.lastName()
        phone: vm.patient.phone().replace(/[(|)|-]/g, "").trim()
      $.post(apiUrl.registerUrl, JSON.stringify(patient))
      .fail handleError
      .done (response) ->
        vm.customerId(response)
        ko.mapping.fromJS(defaultPatient, {}, vm.patient)
        $thankYou.modal('show')

  checkSize = (el) ->
    if el.value.length > 0
      $label.removeClass 'move-top'
      $label.addClass 'move-top'
    else
      $label.removeClass 'move-top'


  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else 3
    vm.labels[fieldName][index]

  vm.labels =
    welcome: [
      "Welcome to Smart Medical!"
      "Добро пожаловать в Smart Medical!"
      "Smart Medical-ga xush kelibsiz!"
    ]
    closeModal: [
      "Close"
      "Закрыть"
      "Yopish"
    ]
    registrationForm: [
      "Registration Form"
      "Форма регистрации"
      "Ro'yxatdan o'tish shakli"
    ]
    register: [
      "Register"
      "Регистрация"
      "Ro'yxatdan o'tish"
    ]
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
    phoneNumber: [
      "Phone number"
      "Телефонный номер"
      "Telefon raqami"
    ]
    thankYou: [
      "Thank you!"
      "Спасибо!"
      "Rahmat! Siz ro'yxatdan o'tdingiz!"
    ]
    yourID: [
      "You are registered on ID:"
      "Вы зарегистрированы по ID:"
      "Sizning ID:"
    ]

  ko.applyBindings {vm}