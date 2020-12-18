$ ->
  my.initAjax()

  Glob = window.Glob || {}

  apiUrl =
    get: '/get-patient'
    delete: '/delete-patient'
    getRegion: '/get-region'
    getTown: '/get-town'
    getOrganization: '/getOrganizationName'
    getWorkType: '/get-department-for-organization'
    getUpdatePatientInfo: '/patient/update-dashboard'
    updatePatient: '/update-patient'

  defaultPatientData =
    firstName: ''
    middleName: ''
    lastName: ''
    passport_sn: ''
    gender: ''
    birthday: ''
    address: ''
    phone: ''
    cardNumber: ''
    photo: ''

  defaultSelectedPatientData =
    address: ''
    birthday: ''
    cardNumber: ''
    city: ''
    firstName: ''
    fullname: ''
    gender: ''
    id: ''
    lastCheckup: ''
    lastName: ''
    middleName: ''
    organizationName: ''
    passport_sn: ''
    phone: ''
    region: ''
    specPartJson:
      region: ''
      city: ''
      department: ''
    workTypeId: ''

  vm = ko.mapping.fromJS
    patient: defaultPatientData
    enableSubmitButton: yes
    patientList: []
    selectedRegion: []
    regionList: []
    selectedTown: []
    townList: []
    selectedOrganization: []
    organizationList: []
    selectedWorkType: []
    workTypeList: []
    selectPatient: defaultSelectedPatientData
    language: Glob.language

  $('.datepicker').datepicker({
    clearBtn: true,
    format: "dd/mm/yyyy"
  })
  $updatePaatienInfoModal = $('updatePatientInfoModal')

  $('#reservationDate').on 'change', () ->
    pickedDate = $('input').val();
    $('#pickedDate').html(pickedDate)
    $('.datepicker-dropdown').hide()

  handleError = (error) ->
    if error.status is 500 or (error.status is 400 and error.responseText)
      toastr.error(error.responseText)
    else
      toastr.error('Something went wrong! Please try again.')

  $contentFile = $('input[name=attachedFile]')
  $contentFile.change ->
    filePath = $(this).val()
    fileName = filePath.replace(/^.*[\\\/]/, '')

    reAllowedTypes = /^.+((\.jpg)|(\.jpeg)|(\.png))$/i
    if !reAllowedTypes.test(fileName)
      alert('Only PNG or JPG files are allowed.')
      return false

    $('#file-name').html fileName
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
      vm.enableSubmitButton(yes)
    done: (e, data) ->
      result = data.result
      if result is 'OK'
        toastr.success('Form has been successfully submitted!')
        ko.mapping.fromJS(defaultPatientData, {}, vm.patient)
        vm.selectedRegion('')
        vm.selectedTown('')
        vm.selectedOrganization('')
        vm.selectedWorkType('')
      else
        vm.enableSubmitButton(yes)
        toastr.error(result or 'Something went wrong! Please try again.')

  vm.onSubmit = ->
    toastr.clear()
    if (!vm.patient.firstName())
      toastr.error("Ko'rikdan o'tuvchi shaxs nomini kiriting!")
      return no
    else if (!vm.patient.middleName())
      toastr.error("Ko'rikdan o'tuvchi shaxs sharfini kiriting!")
      return no
    else if (!vm.patient.lastName())
      toastr.error("Ko'rikdan o'tuvchi shaxs familiyasini kiriting!")
      return no
    else if (!vm.patient.passport_sn())
      toastr.error("Ko'rikdan o'tuvchi shaxs passport nomerini kiriting!")
      return no
    else if (!vm.patient.birthday())
      toastr.error("Ko'rikdan o'tuvchi shaxs tug'ilgan sanasini kiriting!")
      return no
    else if (!vm.patient.gender())
      toastr.error("Ko'rikdan o'tuvchi shaxs jinsini tanlang!")
      return no
    else if (!vm.patient.phone())
      toastr.error("Ko'rikdan o'tuvchi shaxs telefon raqamini kiriting!")
      return no
    else if (!vm.selectedRegion())
      toastr.error("Ko'rikdan o'tuvchi shaxs yashash manzilini tanlang!")
      return no
    else if (!vm.selectedTown())
      toastr.error("Ko'rikdan o'tuvchi shaxs shahar yoki tumanni tanlang!")
      return no
    else if (!vm.patient.address())
      toastr.error("Ko'rikdan o'tuvchi shaxs mamzilini kiriting!")
      return no
    else if (!vm.selectedOrganization())
      toastr.error("Ko'rikdan o'tuvchi shaxs tashkiloti nomini tanlang!")
      return no
    else if (!vm.selectedWorkType())
      toastr.error("Ko'rikdan o'tuvchi shaxs ish turini tanlang!")
      return no
    else if (!vm.patient.cardNumber())
      toastr.error("Ko'rikdan o'tuvchi shaxs karta raqamini kiriting!")
      return no
    if formData
      vm.enableSubmitButton(no)
      formData.submit()
    else
      $fileUploadForm.fileupload('send', {files: ''})
      return no

  getPatient = ->
    $.get(apiUrl.get)
    .fail(handleError)
    .done (response) ->
      for res in response
        res.fullname = res.lastName + ' ' + res.firstName + ' '+ res.middleName
      vm.patientList response
  getPatient()

  getRegion = ->
    $.get(apiUrl.getRegion)
    .fail(handleError)
    .done (response) ->
      vm.regionList(response)

  getRegion()

  getOrganization = ->
    $.get(apiUrl.getOrganization)
    .fail(handleError)
    .done (response) ->
      vm.organizationList(response)

  getOrganization()

  vm.selectedRegion.subscribe (id) ->
    if (id)
      $.post(apiUrl.getTown, JSON.stringify({id: id}))
      .fail handleError
      .done (response) ->
        vm.townList(response)
    else
      vm.townList('')

  vm.selectedOrganization.subscribe (name) ->
    if (name isnt undefined )
      $.post(apiUrl.getWorkType, JSON.stringify({name: name}))
      .fail handleError
      .done (response) ->
        vm.workTypeList(response)

  vm.deletePatient = (id) ->
    data =
      id: id
    $('#delete').modal('show')
    $(document).on 'click', '#ask_delete', ->
      $.ajax
        url: apiUrl.delete
        type: 'DELETE'
        data: JSON.stringify(data)
        dataType: 'json'
        contentType: 'application/json'
      .fail handleError
      .done (response) ->
        $('#close_modal').modal('hide')
        getPatient()
        toastr.success(response)
    $(this).parents('tr').remove()

  vm.convertIntToDate = (intDate)->
    moment(+intDate).format('MMM DD, YYYY')

  vm.convertStrToDate = (strDate) ->
    if strDate
      moment(+strDate).format('MMM DD, YYYY')

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else 1
    vm.labels[fieldName][index]

  vm.updatePatientInfo = (data) -> ->
    console.log(data)
    ko.mapping.fromJS(data, {}, vm.selectPatient)

  vm.updatePatient = ->
    data =
      id: vm.selectPatient.id
      firstName: vm.selectPatient.firstName
      lastName: vm.selectPatient.lastName
      middleName: vm.selectPatient.middleName
      gender: vm.selectPatient.gender
      passport_sn: vm.selectPatient.passport_sn
      phone: vm.selectPatient.phone
      cardNumber: vm.selectPatient.cardNumber
      birthday: vm.selectPatient.birthday
      address: vm.selectPatient.address
    console.log(data)
    $.post(apiUrl.updatePatient, JSON.stringify(data))
    .fail handleError
    .done (response) ->
      toastr.success(response)
      $updatePaatienInfoModal.modal('hide')

  vm.labels =
    label: [
      "Hello World!"
      "Salom Dunyo!"
    ]

  ko.applyBindings {vm}