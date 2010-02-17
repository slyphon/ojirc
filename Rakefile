CLASSES_DIR = File.expand_path('./classes')

def add_to_classpath(*stuff)
  ary = []
  if cp = ENV['CLASSPATH']
    ary += cp.split(':')
  end
  ary.unshift(stuff)
  ary.flatten!
  ary.uniq!

  ENV['CLASSPATH'] = ary.join(':')
end

LIBS = FileList['book', 'lib/**/*.jar']

directory CLASSES_DIR

task :classpath => CLASSES_DIR do
  add_to_classpath(CLASSES_DIR, Dir.getwd, *LIBS)

  if cj_ext_dir = ENV['CLOJURE_EXT']
    exts = Dir["#{cj_ext_dir}/*"]
    add_to_classpath(*exts)
  end

  $stderr.puts "classpath is: #{ENV['CLASSPATH']}"
end

task 'start-ng' => :classpath do
  args = ['java', ENV['CLOJURE_OPTS'], 'com.martiansoftware.nailgun.NGServer', '127.0.0.1']
  exec(args.compact.join(' '))
end


