$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    registerUrl: '/patient/add-patient'
    getAnalysisType: '/patient/get-analysis-type'
    getMrtType: '/patient/get-mrt-type'
    getMsktType: '/patient/get-mskt-type'
    getUziType: '/patient/get-uzi-type'

  defaultPatient =
    firstName: ''
    lastName: ''
    phone: ''
    dateOfBirth: ''
    address: ''
    docFullName: ''
    docPhone: ''
    analysisType: ''
    analysisGroup: ''

  vm = ko.mapping.fromJS
    patient: defaultPatient
    customerId: ''
    getAnalysisTypeList: []
    getMrtTypeList: []
    getMsktTypeList: []
    getUziTypeList: []
    language: Glob.language
    selectedMrt: ''
    selectedMskt: ''
    selectedUzi: ''

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

  getMrtType = ->
    $.get(apiUrl.getMrtType)
    .fail handleError
    .done (response) ->
      vm.getMrtTypeList(response)
  getMrtType()

  getMsktType = ->
    $.get(apiUrl.getMsktType)
    .fail handleError
    .done (response) ->
      vm.getMsktTypeList(response)
  getMsktType()

  getUziType = ->
    $.get(apiUrl.getUziType)
    .fail handleError
    .done (response) ->
      vm.getUziTypeList(response)
  getUziType()

  vm.onSubmit = ->
    toastr.clear()
    clearedPhone = my.clearPhone(vm.patient.phone())
    if !vm.patient.firstName()
      toastr.error("Iltimos ismingizni kiriting!")
      return no
    else if !vm.patient.lastName()
      toastr.error("Iltimos familiyangizni kiriting!")
      return no
    else if !vm.patient.phone()
      toastr.error("Iltimos telefon raqamingizni kiriting!")
    else if vm.patient.phone() and !my.isValidPhone(clearedPhone)
      toastr.error("Iltimos telefon raqamingizni to'gri kiriting!")
      return no
    else if !vm.patient.dateOfBirth()
      toastr.error("Iltimos tug'ilgan yilini kiriting!")
      return no
    else if vm.patient.dateOfBirth() and vm.patient.dateOfBirth().length isnt 10
      toastr.error("Iltimos tug'ilgan yilini to'g'ri kiriting!")
      return no
    else if !vm.patient.address()
      toastr.error("Iltimos, manzilni kiriting!")
      return no
    else if !vm.patient.analysisType()
      toastr.error("Iltimos tahlil turini kiriting!")
      return no
    else if vm.patient.analysisType() is "MRT" and !vm.selectedMrt()
      toastr.error("Iltimos tahlil turini kiriting!")
      return no
    else if vm.patient.analysisType() is "MSKT" and !vm.selectedMskt()
      toastr.error("Iltimos tahlil turini kiriting!")
      return no
    else if vm.patient.analysisType() is "UZI" and !vm.selectedUzi()
      toastr.error("Iltimos tahlil turini kiriting!")
      return no
    else
      data = ko.mapping.toJS vm.patient
      data.phone = clearedPhone
      data.docPhone = my.clearPhone(vm.patient.docPhone())
      data.companyCode = window.location.host
      data.analysisGroup =
        if vm.patient.analysisType() is "MRT"
          vm.selectedMrt()
        else if vm.patient.analysisType() is "MSKT"
          vm.selectedMskt()
        else if vm.patient.analysisType() is "UZI"
          vm.selectedUzi()
      $.post(apiUrl.registerUrl, JSON.stringify(data))
      .fail handleError
      .done (response) ->
        vm.customerId(response)
        ko.mapping.fromJS(defaultPatient, {}, vm.patient)
        $thankYou.modal('show')

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
    dateOfBirth: [
      "Date of birth (day/month/year)"
      "Дата рождения (den/mesets/god)"
      "Tug'ilgan yili (kun/oy/yil)"
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
    mrtType: [
      "MRT type"
      "Тип МРТ"
      "MRT turi"
    ]
    msktType: [
      "MSKT type"
      "Тип МСКТ"
      "MSKT turi"
    ]
    uziType: [
      "Ultrasound type"
      "Узи типа"
      "UZI turi"
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