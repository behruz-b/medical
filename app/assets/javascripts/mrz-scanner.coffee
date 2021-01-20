root = exports ? this

root.mrz =
  scanner: (image) ->
    scanner = new MRZ.Scanner
    array = scanner.parseImage(image).split('\n')
    document = new MRZ.Document(array.slice(array.length - 3, array.length).join('\n'))
    result = JSON.stringify(document.parse(), null, 2)
    return result