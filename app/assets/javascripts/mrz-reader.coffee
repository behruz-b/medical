root = exports ? this
$ ->

root.mrz =
  scanner: (image) ->
    scanner = new (MRZ.Scanner)
    value = scanner.parseImage(image)
    array = value.split('\n')
    text = array.slice(array.length - 3, array.length).join('\n')
    document = new (MRZ.Document)(text)
    result = document.parse()
    return JSON.stringify(result, null, 2)