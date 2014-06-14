match_name = /^\s(\S.*)$/
match_synonym = /^\s\s{(\S.*)}$/

def create_percolator(bird) 
  puts bird
  all_names = "\"#{bird[:name]}\" \"#{bird[:synonym].join('\" \"')}\""
  @client.index index: @index,
      type: '.percolator',
      id: bird[:name],
      body: { 
        query: { 
          query_string: { query: all_names } 
        }, 
        bird: bird[:name], 
        synonyms: bird[:synonym] 
      }
end

File.open("../input/LR_USA_List_Flat_20081103.txt", "r") do |file_handle|
  bird = nil
  file_handle.each_line do |line|
    m = match_name.match(line)
    if m
      unless bird == nil
        create_percolator bird
      end
      bird = {:name => m[1], :synonym => []}
    else
      m = match_synonym.match(line)
      if m
        m[1].split(', ').each do |s|
          bird[:synonym] << s
        end
      end
    end
  end
end

File.open("../input/Hotspots Nova Scotia.txt", "r") do |file_handle|
  file_handle.each_line do |line|
    line.chop!
    puts line
    @client.index index: @index,
        type: '.percolator',
        id: line,
        body: { query: { query_string: { query: "\"#{line}\"" } }, location: line }
  end
end
