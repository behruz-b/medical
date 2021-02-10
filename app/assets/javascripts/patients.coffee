$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    patientsUrl: '/patient/get-patients'

  vm = ko.mapping.fromJS
    language: Glob.language
    patients: []
    customerId: ''

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

  vm.convertMonthToDayDate = (date) ->
    moment(date).format('DD/MM/YYYY')

  vm.showFullImage = (customerId) -> ->
    vm.customerId(customerId)
    $('#analysisImage').modal('show')

  getPatients = ->
    $.get(apiUrl.patientsUrl)
    .fail handleError
    .done (response) ->
      vm.patients(response)
  getPatients()

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else 3
    vm.labels[fieldName][index]

  vm.labels =
    patients: [
      "Patients"
      "Пациенты"
      "Bemorlar"
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
    dateOfBirth: [
      "Date of birth"
      "Дата рождения"
      "Tug'ilgan yili"
    ]
    phoneNumber: [
      "Phone number"
      "Телефонный номер"
      "Telefon raqami"
    ]
    createdAt: [
      "Created at"
      "Время регистрации"
      "Ro'yhatdan o'tgan vaqti"
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
    analysisGroup: [
      "Analysis group"
      "Группа анализа"
      "Tahlil guruhi"
    ]
    smsLinkClick: [
      "Result SMS"
      "Результат СМС"
      "SMS natijasi"
    ]
    analysisResult: [
      "Analysis result"
      "Результат анализа"
      "Tahlil natijasi"
    ]
    closeModal: [
      "Close"
      "Закрыть"
      "Yopish"
    ]

  ko.applyBindings {vm}