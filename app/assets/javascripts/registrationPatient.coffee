$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    registerUrl: '/patient/add-patient'
    getAnalysisType: '/patient/get-analysis-type'
    getMrtType: '/patient/get-mrt-type'
    getMsktType: '/patient/get-mskt-type'
    getUziType: '/patient/get-uzi-type'
    getLabType: '/patient/get-lab-type'
    getPatientsDoc: '/patient/get-patients-doc'
    searchByPatientName: '/patient/search-patient-name'

  defaultPatient =
    firstName: ''
    lastName: ''
    phone: ''
    dateOfBirth: ''
    address: ''
    docFullName: ''
    docPhone: ''
    docId: ''
    analysisType: ''
    analysisGroup: ''

  vm = ko.mapping.fromJS
    patient: defaultPatient
    customerId: ''
    getAnalysisTypeList: []
    getMrtTypeList: []
    getMsktTypeList: []
    getUziTypeList: []
    getLabTypeList: []
    getPatientsDocList: []
    language: Glob.language
    selectedMrt: ''
    selectedMskt: ''
    selectedUzi: ''
    selectedLaboratory: ''
    searchByPatientNameReport: []

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

  getPatientsDoc = ->
    $.get(apiUrl.getPatientsDoc)
    .fail handleError
    .done (response) ->
      vm.getPatientsDocList(response)
  getPatientsDoc()

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

  getLabType = ->
    $.get(apiUrl.getLabType)
    .fail handleError
    .done (response) ->
      vm.getLabTypeList(response)
  getLabType()

  vm.patient.firstName.subscribe (e) ->
    if e
      $.post('/patient/search-patient-name/' + e)
      .fail handleError
      .done (response) ->
        vm.searchByPatientNameReport(response)

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
    else if vm.patient.dateOfBirth() and !my.isValidDate(vm.patient.dateOfBirth())
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
    else if vm.patient.analysisType() is "Laboratoriya" and !vm.selectedLaboratory()
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
        else if vm.patient.analysisType() is "Laboratoriya"
          vm.selectedLaboratory()
      docInfo = vm.getPatientsDocList().filter (x) -> x.id == data.docId
      data.docFullName = docInfo[0]?.fullname
      data.docPhone = docInfo[0]?.phone
      my.blockUI()
      $.post(apiUrl.registerUrl, JSON.stringify(data))
      .fail handleError
      .done (response) ->
        vm.customerId(response)
        ko.mapping.fromJS(defaultPatient, {}, vm.patient)
        $thankYou.modal('show')

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else if vm.language() is 'cy' then 3 else 4
    vm.labels[fieldName][index]

  vm.labels =
    welcome: [
      "Welcome to Smart Medical!"
      "Добро пожаловать в Smart Medical!"
      "Smart Medical-ga xush kelibsiz!"
      "Smart Medical-га ҳуш келибсиз!"
    ]
    closeModal: [
      "Close"
      "Закрыть"
      "Yopish"
      "Ёпиш"
    ]
    registrationForm: [
      "Registration Form"
      "Форма регистрации"
      "Ro'yxatdan o'tish shakli"
      "Рўйҳатдан ўтиш шакли"
    ]
    register: [
      "Register"
      "Регистрация"
      "Ro'yxatdan o'tish"
      "Рўйҳатдан ўтиш"
    ]
    firstName: [
      "First name"
      "Имя"
      "Ism"
      "Исм"
    ]
    lastName: [
      "Last name"
      "Фамилия"
      "Familiya"
      "Фамилия"
    ]
    phoneNumber: [
      "Phone number"
      "Телефонный номер"
      "Telefon raqami"
      "Телефон рақами"
    ]
    selectDoctor: [
      "Select doctor"
      "Выберите врача"
      "Shifokorni tanlang"
      "Шифокорни танланг"
    ]
    docFullName: [
      "Doctor's full name"
      "ФИО врача"
      "Shifokorning to'liq ismi"
      "Шифокорнинг тўлиқ исми"
    ]
    docPhone: [
      "Doctor's phone number"
      "Телефон врача"
      "Shifokorning telefon raqami"
      "Шифокорнинг телефон рақами"
    ]
    dateOfBirth: [
      "Date of birth (day/month/year)"
      "Дата рождения (den/mesets/god)"
      "Tug'ilgan yili (kun/oy/yil)"
      "Туғилган йили (кун/ой/йил)"
    ]
    address: [
      "Address"
      "Адрес"
      "Manzil"
      "Манзил"
    ]
    analysisType: [
      "Analysis type"
      "Тип анализа"
      "Tahlil turi"
      "Таҳлил тури"
    ]
    mrtType: [
      "MRT type"
      "Тип МРТ"
      "MRT turi"
      "МРТ тури"
    ]
    msktType: [
      "MSKT type"
      "Тип МСКТ"
      "MSKT turi"
      "МСКТ тури"
    ]
    uziType: [
      "Ultrasound type"
      "Узи типа"
      "UZI turi"
      "УЗИ тури"
    ]
    labType: [
      "Laboratory type"
      "Тип лаборатории"
      "Laboratoriya turi"
      "Лаборатория тури"
    ]
    thankYou: [
      "Thank you!"
      "Спасибо!"
      "Rahmat! Siz ro'yxatdan o'tdingiz!"
      "Раҳмат! Сиз рўйҳатдан ўтдингиз!"
    ]
    yourID: [
      "You are registered on ID:"
      "Вы зарегистрированы по ID:"
      "Sizning ID:"
      "Сизнинг ID:"
    ]
    existingPatients: [
      "Existing patients"
      "Существующие пациенты"
      "Mavjud bemorlar"
      "Мавжуд беморлар"
    ]

  ko.applyBindings {vm}