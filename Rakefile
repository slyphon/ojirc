CLASSES_DIR = File.expand_path('./classes')

LIBS = FileList['book', 'lib/**/*.jar']

LOG4J_PROPERITES = 'lib/log4j.properties'

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

def log4j_props
  File.exists?(LOG4J_PROPERITES) ? "-Dlog4j.configuration=#{LOG4J_PROPERITES}" : nil
end

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
  args = ['java', ENV['CLOJURE_OPTS'], log4j_props, 'com.martiansoftware.nailgun.NGServer', '127.0.0.1']
  args.compact!
  $stderr.puts "running: #{args.compact.join(' ')}"
  exec(*args)
end

task 'ngircd' do
  sh "ngircd -f config/ngircd.conf --nodaemon --passive"
end

