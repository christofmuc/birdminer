Dir.glob('../downloaded/**/*').reject{|file| File.directory? file}.each do |file|
    puts "Indexing #{file}"    
    @client.index index: @index,
        type: @type,
        body: {
            fulltext: Nokogiri::HTML(CGI.unescapeHTML(IO.read(file).force_encoding("ISO-8859-1").encode("UTF-8"))).content
        }
end

