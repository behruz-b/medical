$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    changePassword: '/doctor/add-password'

  defaultDoctor =
    newPass: ''
    repeatPass: ''

  vm = ko.mapping.fromJS
    doctor: defaultDoctor
    language: Glob.language

  handleError = (error) ->
    $.unblockUI()
    if error.status is 401
      toastr.error(error.responseText)
      my.logout()
    else if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  $('#show_hide_password a').on 'click', (event) ->
    event.preventDefault()
    if $('#show_hide_password input').attr('type') == 'text'
      $('#show_hide_password input').attr 'type', 'password'
      $('#show_hide_password i').addClass 'fa-eye-slash'
      $('#show_hide_password i').removeClass 'fa-eye'
    else if $('#show_hide_password input').attr('type') == 'password'
      $('#show_hide_password input').attr 'type', 'text'
      $('#show_hide_password i').removeClass 'fa-eye-slash'
      $('#show_hide_password i').addClass 'fa-eye'

  $('#show_hide_repeat_password a').on 'click', (event) ->
    event.preventDefault()
    if $('#show_hide_repeat_password input').attr('type') == 'text'
      $('#show_hide_repeat_password input').attr 'type', 'password'
      $('#show_hide_repeat_password i').addClass 'fa-eye-slash'
      $('#show_hide_repeat_password i').removeClass 'fa-eye'
    else if $('#show_hide_repeat_password input').attr('type') == 'password'
      $('#show_hide_repeat_password input').attr 'type', 'text'
      $('#show_hide_repeat_password i').removeClass 'fa-eye-slash'
      $('#show_hide_repeat_password i').addClass 'fa-eye'

  vm.newPassword = ->
    toastr.clear()
    if !vm.doctor.newPass()
      toastr.error("Iltimos yangi parolni kiriting!")
      return no
    else if !vm.doctor.repeatPass()
      toastr.error("Iltimos yangi parolni to'g'ri kiriting!")
      return no
    else if (vm.doctor.repeatPass() != vm.doctor.newPass())
      toastr.error("Iltimos yangi parolni to'g'ri takrorlang!")
      return no
    else if !my.passValidation(vm.doctor.repeatPass())
      toastr.error("Iltimos yangi parolda kamida 1 ta raqam, 1 ta katta kichik harf va belgi bulishi kerak!")
    else
      my.blockUI()
      $.post(apiUrl.changePassword, JSON.stringify(newPass: vm.doctor.newPass()))
      .fail handleError
      .done () ->
        toastr.success("Muvaffaqiyatli yakunlandi!")
        ko.mapping.fromJS(defaultDoctor, {}, vm.doctor)
        my.logout()
        $.unblockUI()

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else if vm.language() is 'cy' then 3 else 4
    vm.labels[fieldName][index]

  vm.labels =
    forgotPassword: [
      "Update Password"
      "Обновить пароль"
      "Parolni yangilash"
      "Обновить пароль"
    ]
    login: [
      "Login"
      "Авторизоваться"
      "Login"
      "Логин"
    ]
    newPassword: [
      "New Password"
      "Новый Пароль"
      "Yangi Parol"
      "Янги Пароль"
    ]
    repeatPassword: [
      "Repeat Password"
      "Повтори Пароль"
      "Parolni takrorlang"
      "Паролни такрорланг"
    ]

  ko.applyBindings {vm}