$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    patientsUrl: '/patient/get-patients'

  defaultPatientFilter =
    startDate: ''
    endDate: ''
    analyseType: ''

  vm = ko.mapping.fromJS
    language: Glob.language
    patientsList: []
    customerId: ''
    patientsFilter: defaultPatientFilter

  pageSize = 30
  currentPage = 1
  $pagination = {}
  $paginationEl = $('#pagination')

  initPagination = (total, startPage = 1) ->
    totalPages = Math.ceil(total / pageSize)
    if totalPages < 1
      return no
    $paginationEl.show()
    $paginationEl.twbsPagination(
      startPage: Math.min(startPage, totalPages)
      totalPages: totalPages
      visiblePages: 5
      hideOnlyOnePage: true
      onPageClick: (event, page) ->
        if currentPage isnt page
          vm.getPatients(null, page)
    )
    $pagination = $paginationEl.data('twbsPagination')

  handleError = (error) ->
    $.unblockUI()
    if error.status is 401
      my.logout()
    else if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  vm.convertStringToDate = (stringDate) ->
    moment(stringDate).format('DD/MM/YYYY HH:mm:ss')

  vm.convertMonthToDayDate = (date) ->
    moment(date).format('DD/MM/YYYY')

  vm.showFullImage = (customerId) -> ->
    console.log('cc: ', customerId)
    vm.customerId(customerId)
    console.log('customerId: ', vm.customerId())
    $('#analysisImage').modal('show')

  $('#datetimepicker1').datetimepicker
    format: 'yyyy-mm-dd HH:mm:ss'
    autoclose: true

  $('#datetimepicker2').datetimepicker
    format: 'yyyy-mm-dd HH:mm:ss'
    autoclose: true

  vm.patientsFilter.analyseType.subscribe ->
    vm.getPatients(null, 1)

  vm.getPatients = (page) ->
    console.log('page', page)
    pageParam = "pageSize=#{pageSize}"
    if page
      pageParam += "&page=#{page}"
    reqUrl = "#{apiUrl.patientsUrl}?#{pageParam}"
    patient =
      analyseType: vm.patientsFilter.analyseType()
      startDate: vm.patientsFilter.startDate()
      endDate: vm.patientsFilter.endDate()
    console.log(patient)
    $.post(reqUrl, JSON.stringify(patient))
    .fail handleError
    .done (response) ->
      $pagination.destroy?()
      initPagination(response.total, page)
      vm.patientsList(response.items)
      console.log(vm.patientsList())

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else if vm.language() is 'cy' then 3 else 4
    vm.labels[fieldName][index]

  vm.labels =
    patients: [
      "Patients"
      "Пациенты"
      "Bemorlar"
      "Беморлар"
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
    email: [
      "Email"
      "Эл. адрес"
      "Email"
      "Емаил"
    ]
    dateOfBirth: [
      "Date of birth"
      "Дата рождения"
      "Tug'ilgan yili"
      "Туғилган йили"
    ]
    phoneNumber: [
      "Phone number"
      "Телефонный номер"
      "Telefon raqami"
      "Телефон рақами"
    ]
    createdAt: [
      "Created at"
      "Время регистрации"
      "Ro'yhatdan o'tgan vaqti"
      "Рўйҳатдан ўтган вақти"
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
    analysisGroup: [
      "Analysis group"
      "Группа анализа"
      "Tahlil guruhi"
      "Таҳлил гуруҳи"
    ]
    smsLinkClick: [
      "Result SMS"
      "Результат СМС"
      "SMS natijasi"
      "СМС натижаси"
    ]
    analysisResult: [
      "Analysis result"
      "Результат анализа"
      "Tahlil natijasi"
      "Таҳлил натижаси"
    ]
    closeModal: [
      "Close"
      "Закрыть"
      "Yopish"
      "Ёпиш"
    ]

  ko.applyBindings {vm}