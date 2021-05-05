def setDescription() { 
  def item = Jenkins.instance.getItemByFullName(env.JOB_NAME) 
  item.setDescription("<h5><span style=\"color:green\">This jobs performs some adjustments on the /etc/sysctl.conf file in order to improve the network peformance<ol><li>net.core.wmem_max = 209715200</li><li>net.core.rmem_max = 209715200</li><li>net.ipv4.tcp_rmem = 5242880 10485760 209715200</li><li>net.ipv4.tcp_wmem = 5242880 10485760 209715200</li><li>net.ipv4.tcp_window_scaling = 1</li><li>net.ipv4.tcp_timestamps = 0</li><li>net.ipv4.tcp_sack = 0</li><li>net.ipv4.tcp_dsack = 0</li><li>net.ipv4.tcp_no_metrics_save = 1</li><li>net.core.netdev_max_backlog = 0000</li></ol></span></h5>") 
  item.save()
  }
setDescription()

pipeline {
	// agent any
	agent { label 'ansible' }
	options {
		ansiColor('gnome-terminal')
		buildDiscarder(logRotator(daysToKeepStr: '7'))
		}

   parameters {
        string(
		name: 'Target_Host', 
		description: '<h5>Target host where this adjusments will take place</h5>'
		)
		password(
		name: 'Host_Password', 
		defaultValue: 'arst@dm1n', 
		description: '<h5>Host root\'s password. There is no default password so you must provided by clicking on \"Change Password\".</h5>'
		)
    }
            	
	stages {
		stage("Sleep for Update") {
			steps {
				echo "sleeping for 10 secs to give me time to get the changes and then stop the job"
				sleep 10
			}
		}

		stage('Creating Inventory File') {
			steps {
				sh '''
					echo -e "[nodes]\\n" >  ${WORKSPACE}/inventory.ini | cat ${WORKSPACE}/inventory.ini
					echo ${Target_Host} | sed \'s/,/\\n/g\' | while read line ; do sed -i \'/\\[nodes\\]/a \\\'"${line}"\'\' ${WORKSPACE}/inventory.ini ; done
				'''
			}
		}

		stage('Running Ansible Playbook') {
			steps {
				ansiblePlaybook(
				playbook: '${WORKSPACE}/improve_network_performance.yaml',
				inventory: '${WORKSPACE}/inventory.ini',
				colorized: true,
				extras: '-vv -f 30 --ssh-extra-args=" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"',
				extraVars: [
				jenkins_workspace: '${WORKSPACE}/',
				ansible_password: [value: '${Host_Password}', hidden: true]
				])
			}
		}
		
}
  
	post {
		always {
			echo 'Clenning up the workspace'
			deleteDir()
		}
		}
}
