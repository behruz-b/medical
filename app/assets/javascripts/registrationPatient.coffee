$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    registerUrl: '/patient'
    patientsUrl: '/patients'
    statsUrl: '/stats'

  defaultPatient =
    firstName: ''
    lastName: ''
    passportSeries: ''
    passportNumber: ''
    email: ''
    phone: ''

  vm = ko.mapping.fromJS
    patient: defaultPatient
    customerId: ''
    language: Glob.language
    patients: []
    stats: []

  handleError = (error) ->
    if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  $thankYou = $('#thankYou')

  vm.getPatients = ->
    $.get(apiUrl.patientsUrl)
    .fail handleError
    .done (response) ->
      vm.patients(response)

  vm.getStats = ->
    $.get(apiUrl.statsUrl)
      .fail handleError
      .done (response) ->
        vm.stats(response)

  vm.onSubmit = ->
    toastr.clear()
    if !vm.patient.firstName()
      toastr.error("Iltimos ismingizni kiriting!")
      return no
    else if !vm.patient.lastName()
      toastr.error("Iltimos familiyangizni kiriting!")
      return no
    else if vm.patient.email() and !my.isValidEmail(vm.patient.email())
      toastr.error("Iltimos emailni to'gri kiriting!")
      return no
    else if !vm.patient.phone()
      toastr.error("Iltimos telefon raqamingizni kiriting!")
    else if vm.patient.phone() and !my.isValidPhone(vm.patient.phone().replace(/[(|)|-]/g, "").trim())
      toastr.error("Iltimos telefon raqamingizni to'gri kiriting!")
      return no
    else if !vm.patient.passportSeries()
      toastr.error("Iltimos passport seriasini kiriting!")
      return no
    else if vm.patient.passportSeries() and vm.patient.passportSeries().length != 2
      toastr.error("Iltimos passport seriasini to'liq kiriting!")
      return no
    else if !vm.patient.passportNumber()
      toastr.error("Iltimos passport raqamini kiriting!")
      return no
    else if vm.patient.passportNumber() and vm.patient.passportNumber().length != 7
      toastr.error("Iltimos passport raqamini to`liq kiriting!")
      return no
    else
      patient =
        firstName: vm.patient.firstName()
        lastName: vm.patient.lastName()
        passportSn: vm.patient.passportSeries().toUpperCase() + vm.patient.passportNumber()
        email: vm.patient.email()
        phone: vm.patient.phone().replace(/[(|)|-]/g, "").trim()
      $.post(apiUrl.registerUrl, JSON.stringify(patient))
      .fail handleError
      .done (response) ->
        vm.customerId(response)
        ko.mapping.fromJS(defaultPatient, {}, vm.patient)
        $thankYou.modal('show')

  $label = $('#passport_sn')
  $pNumber = document.getElementById('p_number')
  $pSeries = document.getElementById('p_series')

  checkSize = (el) ->
    if el.value.length > 0
      $label.removeClass 'move-top'
      $label.addClass 'move-top'
    else
      $label.removeClass 'move-top'

  $pNumber.addEventListener 'focusin', (_) ->
    $label.addClass 'move-top'

  $pNumber.addEventListener 'focusout', (event) ->
    checkSize event.target

  $pSeries.addEventListener 'focusin', (_) ->
    $label.addClass 'move-top'

  $pSeries.addEventListener 'focusout', (event) ->
    checkSize event.target

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