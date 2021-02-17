$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    addPatientsDocUrl: '/patient/add-patients-doc'
    getPatientsDoc: '/patient/get-patients-doc'

  defaultPatientsDoc =
    fullName: ''
    phone: ''

  vm = ko.mapping.fromJS
    patientsDoc: defaultPatientsDoc
    getPatientsDocList: []
    language: Glob.language

  handleError = (error) ->
    $.unblockUI()
    if error.status is 401
      my.logout()
    else if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  getPatientsDoc = ->
    $.get(apiUrl.getPatientsDoc)
    .fail handleError
    .done (response) ->
      vm.getPatientsDocList(response)
  getPatientsDoc()

  vm.onSubmit = ->
    toastr.clear()
    clearedPhone = my.clearPhone(vm.patientsDoc.phone())
    if !vm.patientsDoc.fullName()
      toastr.error("Iltimos, shifokorning to'liq ismini kiriting!")
    else if !vm.patientsDoc.phone()
      toastr.error("Iltimos, shifokorning telefon raqamini kiriting!")
    else if vm.patientsDoc.phone() and !my.isValidPhone(clearedPhone)
      toastr.error("Iltimos, shifokorning telefon raqamini to'g'ri kiriting!")
      return no
    else
      data = ko.mapping.toJS vm.patientsDoc
      data.phone = clearedPhone
      my.blockUI()
      console.log(data)
      $.post(apiUrl.addPatientsDocUrl, JSON.stringify(data))
      .fail handleError
      .done () ->
        $.unblockUI()
        ko.mapping.fromJS(defaultPatientsDoc, {}, vm.patientsDoc)
        getPatientsDoc()
        toastr.success("Shifokor muoffaqiyatli qo'shildi")

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else if vm.language() is 'cy' then 3 else 4
    vm.labels[fieldName][index]

  vm.labels =
    patientsDocTable: [
      "List of doctors"
      "Список врачей"
      "Shifokorlar ro'yxati"
      "Шифокорлар рўйҳати"
    ]
    registrationForm: [
      "Registration"
      "Форма регистрации"
      "Ro'yxatdan o'tkazish"
      "Рўйҳатдан ўтказиш"
    ]
    send: [
      "Send"
      "Отправить"
      "Jo'natish"
      "Жўнатиш"
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

  ko.applyBindings {vm}