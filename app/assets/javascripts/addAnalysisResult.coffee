$ ->
  my.initAjax()

  Glob = window.Glob || {}

  defaultPatient =
    id: ''
    file: ''

  vm = ko.mapping.fromJS
    patient: defaultPatient
    enableSubmitButton: no
    language: Glob.language

  readURL = (input) ->
    if input.files and input.files[0]
      reader = new FileReader
      reader.onload = (e) ->
        $('#show-image').attr 'src', e.target.result

      reader.readAsDataURL input.files[0]

  $('#capture-image').change ->
    readURL this

  handleError = (error) ->
    $.unblockUI()
    if error.status is 401
      my.logout()
    else if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  $contentFile = $('input[name=file]')
  $contentFile.change ->
    filePath = $(this).val()
    fileName = filePath.replace(/^.*[\\\/]/, '')

    reAllowedTypes = /^.+((\.jpg)|(\.jpeg)|(\.png))$/i
    if !reAllowedTypes.test(fileName)
      alert('Only PNG or JPG files are allowed.')
      return false
    vm.enableSubmitButton yes

  formData = null
  $fileUploadForm = $('#patient-form')
  $fileUploadForm.fileupload
    dataType: 'text'
    autoUpload: no
    singleFileUploads: false
    replaceFileInput: true
    multipart: true
    add: (e, data) ->
      formData = data
    fail: (e, data) ->
      handleError(data.jqXHR)
      vm.enableSubmitButton(no)
    done: (e, data) ->
      result = data.result
      ko.mapping.fromJS(defaultPatient, {}, vm.patient)
      $.unblockUI()
      $('#show-image').attr 'src', ''
      toastr.success(result)
      vm.enableSubmitButton(no)

  vm.onSubmit = ->
    toastr.clear()
    if !vm.patient.id()
      toastr.error("Iltimos id ni kiriting!")
      return no
    else if !vm.patient.file()
      toastr.error("Iltimos faylni kiriting!")
      return no
    else if formData
      my.blockUI()
      vm.enableSubmitButton(no)
      formData.submit()

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else if vm.language() is 'cy' then 3 else 4
    vm.labels[fieldName][index]

  vm.labels =
    addAnalysisResult: [
      "Add Analysis Result"
      "Добавить результат анализа"
      "Tahlil natijasini qo'shish"
      "Таҳлил натижасини қўшиш"
    ]
    file: [
      "File"
      "Файл"
      "Fayl"
      "Файл"
    ]
    submit: [
      "Submit"
      "Отправить"
      "Jo'natish"
      "Жўнатиш"
    ]

  ko.applyBindings {vm}