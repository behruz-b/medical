$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    registerUrl: '/createPatient'

  vm = ko.mapping.fromJS
    firstName: ''
    lastName: ''
    passportSeries: ''
    passportNumber: ''
    email: ''
    phone: ''
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
    if !vm.firstName()
      toastr.error("Iltimos ismingizni kiriting!")
      return no
    else if !vm.lastName()
      toastr.error("Iltimos familiyangizni kiriting!")
      return no
    else if vm.email() and !my.isValidEmail(vm.email())
      toastr.error("Iltimos emailni to'gri kiriting!")
      return no
    else if !vm.phone()
      toastr.error("Iltimos telefon raqamingizni kiriting!")
    else if vm.phone() and !my.isValidPhone(vm.phone().replace(/[(|)|-]/g, "").trim())
      toastr.error("Iltimos telefon raqamingizni to'gri kiriting!")
      return no
    else if !vm.passportSeries()
      toastr.error("Iltimos passport seriasini kiriting!")
      return no
    else if !vm.passportNumber()
      toastr.error("Iltimos passport raqamini kiriting!")
      return no
    else
      patient =
        firstName: vm.firstName()
        lastName: vm.lastName()
        passportSn: vm.passportSeries().toUpperCase() + vm.passportNumber()
        email: vm.email()
        phone: vm.phone()
      $.post(apiUrl.registerUrl, JSON.stringify(patient))
      .fail handleError
      .done (response) ->
        vm.customerId(response)
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
    toHome: [
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
      "Rahmat!"
    ]
    yourID: [
      "You are registered on ID:"
      "Вы зарегистрированы по ID:"
      "Siz ro'yxatdan o'tdingiz! Sizning ID:"
    ]
    notFound: [
      "Oops! Page not found!"
      "Ой! Страница не найдена!"
      "Afsus! Sahifa topilmadi!"
    ]
    notFoundDescription: [
      "The page you were looking for doesn't exist. You may have mistyped the address or the page may have moved."
      "Страница, которую вы искали, не существует. Возможно, вы ошиблись при вводе адреса или страница могла быть перемещена."
      "Siz izlayotgan sahifa mavjud emas. Ehtimol, siz manzilni noto'g'ri yozgansiz yoki sahifa ko'chib ketgan bo'lishi mumkin."
    ]

  ko.applyBindings {vm}