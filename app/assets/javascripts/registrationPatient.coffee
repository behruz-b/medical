$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    registerUrl: '/createPatient'

  Page =
    home: "home"
    result: "result"

  vm = ko.mapping.fromJS
    firstName: ''
    lastName: ''
    passportSeries: ''
    passportNumber: ''
    email: ''
    phone: ''
    customerId: ''
    language: Glob.language
    page: Page.home

  handleError = (error) ->
    if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  vm.onSubmit = ->
    toastr.clear()
    if !vm.firstName()
      toastr.error("Iltimos ismingizni kiriting!")
      return no
    else if !vm.lastName()
      toastr.error("Iltimos familiyangizni kiriting!")
      return no
    else if !vm.phone()
      toastr.error("Iltimos telefon raqamingizni kiriting!")
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
        passportSn: vm.passportSeries() + vm.passportNumber()
        email: vm.email()
        phone: vm.phone()
      $.post(apiUrl.registerUrl, JSON.stringify(patient))
      .fail handleError
      .done (response) ->
        vm.customerId(response)
        vm.page(Page.result)

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
    name: [
      "First name"
      "Имя"
      "Ism"
    ]

  ko.applyBindings {vm}