$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    registerUrl: '/patient/add-patient'
    getAnalysisType: '/patient/get-analysis-type'

  defaultPatient =
    firstName: ''
    lastName: ''
    phone: ''
    date: ''
    address: ''
    docFullName: ''
    docPhone: ''
    analysisType: ''

  vm = ko.mapping.fromJS
    patient: defaultPatient
    customerId: ''
    getAnalysisTypeList: []
    language: Glob.language

  handleError = (error) ->
    $.unblockUI()
    if error.status is 401
      my.logout()
    else if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  $thankYou = $('#thankYou')

  vm.convertIntToDate = (intDate) ->
    moment(+intDate).format('DD/MM/YYYY')

  getAnalysisType = ->
    $.get(apiUrl.getAnalysisType)
    .fail handleError
    .done (response) ->
      vm.getAnalysisTypeList(response)
  getAnalysisType()

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
    else if !vm.patient.date()
      toastr.error("Iltimos tug'ilgan yilini kiriting!")
      return no
    else if (vm.patient.date() and (vm.patient.date().length < 8 or vm.patient.date().length == 9))
      toastr.error("Iltimos tug'ilgan yilini to'g'ri kiriting!")
      return no
    else if !vm.patient.address()
      toastr.error("Iltimos, manzilni kiriting!")
      return no
    else if !vm.patient.analysisType()
      toastr.error("Iltimos tahlil turini kiriting!")
      return no
    else
      patient =
        firstName: vm.patient.firstName()
        lastName: vm.patient.lastName()
        phone: vm.patient.phone().replace(/[(|)|-]/g, "").trim()
        dateOfBirth: vm.patient.date()
        address: vm.patient.address()
        docFullName: vm.patient.docFullName()
        docPhone: vm.patient.docPhone().replace(/[(|)|-]/g, "").trim()
        analysisType: vm.patient.analysisType()
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
    docFullName: [
      "Doctor's full name"
      "ФИО врача"
      "Shifokorning to'liq ismi"
    ]
    docPhone: [
      "Doctor's phone number"
      "Телефон врача"
      "Shifokorning telefon raqami"
    ]
    date: [
      "Date of birth"
      "Дата рождения"
      "Tug'ilgan yili"
    ]
    address: [
      "Address"
      "Адрес"
      "Manzil"
    ]
    analysisType: [
      "Analysis type"
      "Тип анализа"
      "Tahlil turi"
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